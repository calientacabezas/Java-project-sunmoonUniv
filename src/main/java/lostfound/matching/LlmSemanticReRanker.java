package lostfound.matching;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lostfound.domain.Item;
import lostfound.llm.LlmClient;
import lostfound.privacy.PiiMasker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙 점수 상위 K개를 LLM에 한 번에 보내 의미 유사도(0~100)를 받고,
 * 최종 점수 = 규칙 점수 × 0.8 + 의미 점수 × 0.2 로 다시 정렬한다. 담당: 강채연.
 *
 * <ul>
 *   <li>API 호출은 검색 1회당 1번뿐이다 (물품마다 호출하지 않음).</li>
 *   <li>LLM에는 공개 정보만 보낸다. 비공개 특징(secret)은 절대 보내지 않고, 공개 정보도 마스킹한다.</li>
 *   <li>호출·파싱에 실패하면 받은 목록을 그대로 돌려준다 (규칙 점수만으로 결과 표시).</li>
 * </ul>
 */
public class LlmSemanticReRanker implements SemanticReRanker {

    public static final double RULE_WEIGHT = 0.8;
    public static final double SEMANTIC_WEIGHT = 0.2;

    private static final String SYSTEM_PROMPT = """
            너는 대학교 분실물 매칭 평가자다.
            분실자의 설명과 습득물 후보 목록을 비교해, 각 후보가 분실자의 물건일 가능성을 0~100점으로 매겨라.
            동의어·유사어(예: 버즈=무선 이어폰, 연분홍=분홍, 공대=공학관), 오타, 외국어 표현을 같은 뜻으로 판단하라.
            후보 목록에 없는 ID는 만들지 마라.
            아래 JSON 하나만 출력하라. 마크다운 코드블록이나 설명은 붙이지 마라.
            {"results":[{"id":"ITEM-001","score":85,"reason":"한 줄 근거(한국어)"}]}
            """;

    private final LlmClient llmClient;

    public LlmSemanticReRanker(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public List<MatchResult> rerank(String maskedRawInput, List<MatchResult> topK) {
        if (topK == null || topK.isEmpty() || llmClient == null) {
            return topK;
        }
        try {
            String response = llmClient.complete(SYSTEM_PROMPT, buildUserPrompt(maskedRawInput, topK));
            Map<String, Judgement> judgements = parse(response);
            if (judgements.isEmpty()) {
                System.err.println("[재정렬] 유효한 점수가 없어 규칙 점수만 사용합니다.");
                return topK;
            }
            return combine(topK, judgements);
        } catch (Exception e) {
            System.err.println("[재정렬] LLM 재평가 실패, 규칙 점수만 사용합니다: " + e.getMessage());
            return topK;
        }
    }

    /** 공개 정보만으로 후보 목록 문장을 만든다. */
    static String buildUserPrompt(String maskedRawInput, List<MatchResult> topK) {
        StringBuilder sb = new StringBuilder();
        sb.append("분실자 설명: ").append(PiiMasker.mask(maskedRawInput == null ? "" : maskedRawInput)).append("\n\n");
        sb.append("습득물 후보:\n");
        for (MatchResult r : topK) {
            Item item = r.item();
            String floor = item.getFloor() < 0 ? "지하" + (-item.getFloor()) + "층" : item.getFloor() + "층";
            sb.append(String.format("- id=%s | 종류=%s | 색상=%s | 위치=%s %s | 이름=%s | 속성=%s | 공개특징=%s%n",
                    item.getId(),
                    item.getCategory().label(),
                    item.getColor().label(),
                    item.getBuilding().label(), floor,
                    PiiMasker.mask(item.getTitle()),
                    PiiMasker.mask(item.specificSummary()),
                    PiiMasker.mask(item.getPublicDetail())));
        }
        return sb.toString();
    }

    record Judgement(int score, String reason) {}

    static Map<String, Judgement> parse(String response) {
        Map<String, Judgement> map = new HashMap<>();
        if (response == null) return map;
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        JsonObject root = JsonParser.parseString(cleaned).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        if (results == null) return map;
        for (JsonElement el : results) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("id") || !o.has("score")) continue;
            try {
                String id = o.get("id").getAsString().trim();
                int score = Math.max(0, Math.min(100, (int) Math.round(o.get("score").getAsDouble())));
                String reason = o.has("reason") && !o.get("reason").isJsonNull()
                        ? o.get("reason").getAsString().trim() : "";
                map.put(id, new Judgement(score, reason));
            } catch (RuntimeException ignored) {
                // 한 항목이 잘못되어도 나머지 점수는 사용한다
            }
        }
        return map;
    }

    /** LLM이 점수를 주지 않은 후보는 의미 점수를 규칙 점수와 같게 두어 순위가 흔들리지 않게 한다. */
    static List<MatchResult> combine(List<MatchResult> topK, Map<String, Judgement> judgements) {
        List<MatchResult> combined = new ArrayList<>(topK.size());
        for (MatchResult r : topK) {
            Judgement j = judgements.get(r.item().getId());
            int semantic = (j == null) ? r.ruleScore() : j.score();
            int finalScore = (int) Math.round(r.ruleScore() * RULE_WEIGHT + semantic * SEMANTIC_WEIGHT);
            String reason = (j == null || j.reason().isBlank())
                    ? r.reason()
                    : r.reason() + " | 의미 보정: " + j.reason();
            combined.add(new MatchResult(r.item(), r.ruleScore(), semantic, finalScore, r.details(), reason));
        }
        combined.sort(MatchResult.RANKING);
        return combined;
    }
}
