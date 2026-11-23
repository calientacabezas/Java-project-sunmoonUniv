package lostfound.parsing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmException;
import lostfound.matching.SearchQuery;
import lostfound.privacy.PiiMasker;

import java.time.LocalDate;

/**
 * 자연어 문장을 PiiMasker로 마스킹한 뒤 LLM으로 분석해 표준 SearchQuery로 추출·검증하는 파서.
 * 담당: 김동민.
 */
public class LlmQueryParser implements QueryParser {

    private final LlmClient llmClient;

    public LlmQueryParser(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public SearchQuery parse(String rawInput) {
        String maskedInput = PiiMasker.mask(rawInput == null ? "" : rawInput);
        LocalDate today = LocalDate.now();
        String systemPrompt = """
                너는 선문대학교 교내 분실물 검색 조건 추출기다.
                사용자 문장을 분석해 아래 JSON 형식으로만 응답하라. 마크다운 코드블록(```)이나 설명은 절대 붙이지 마라.
                오늘 날짜는 %s 이다. ("어제", "그제", "오늘" 등 상대 날짜는 YYYY-MM-DD로 계산)

                허용된 enum 값 목록:
                - category: ELECTRONICS, WALLET_CARD, BAG_CLOTHING, GENERAL, UNKNOWN
                - color: WHITE, BLACK, GRAY, SILVER, RED, PINK, ORANGE, YELLOW, BLUE, NAVY, GREEN, PURPLE, BROWN, BEIGE, UNKNOWN
                - building: ENGINEERING(공학관), LIBRARY(도서관), STUDENT_HALL(학생회관), MAIN_HALL(본관), HUMANITIES(인문관), NATURAL_SCIENCE(자연관), WONHWA(원화관), SPORTS_SCIENCE(스포츠과학관), GYMNASIUM(체육관), INDUSTRY_ACADEMY(산학협력관), ORANGE_CAFETERIA(오렌지식당), DORMITORY(기숙사), HEALTH_MEDICAL(보건의료관/평생교육원), UNKNOWN
                - floor: 정수 (지하1층은 -1, 층수 언급이 없으면 null)
                - lostDate: "YYYY-MM-DD" 또는 언급 없으면 null
                - detailText: 물품의 브랜드, 모델명, 특징 키워드 요약 문자열

                출력 JSON 예시:
                {"category":"ELECTRONICS","color":"WHITE","building":"ENGINEERING","floor":3,"lostDate":"%s","detailText":"갤럭시 버즈 이어폰"}
                """.formatted(today, today);

        String rawResponse = llmClient.complete(systemPrompt, maskedInput);
        return parseAndValidateJson(rawResponse, maskedInput, today);
    }

    private SearchQuery parseAndValidateJson(String rawResponse, String maskedInput, LocalDate today) {
        try {
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
            }

            JsonObject obj = JsonParser.parseString(cleaned).getAsJsonObject();

            Category category = parseEnum(obj, "category", Category.class, Category.UNKNOWN);
            ItemColor color = parseEnum(obj, "color", ItemColor.class, ItemColor.UNKNOWN);
            Building building = parseEnum(obj, "building", Building.class, Building.UNKNOWN);
            Integer floor = parseFloor(obj.get("floor"));
            LocalDate lostDate = parseDate(obj.get("lostDate"), today);
            String detailText = getStringOrDefault(obj, "detailText", maskedInput);

            return new SearchQuery(category, color, lostDate, building, floor, detailText, maskedInput);
        } catch (Exception e) {
            throw new LlmException("LLM 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    private <E extends Enum<E>> E parseEnum(JsonObject obj, String key, Class<E> enumType, E fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        try {
            String val = obj.get(key).getAsString().trim().toUpperCase();
            return Enum.valueOf(enumType, val);
        } catch (Exception e) {
            return fallback;
        }
    }

    private Integer parseFloor(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        try {
            int f;
            if (el.getAsJsonPrimitive().isNumber()) {
                f = el.getAsInt();
            } else {
                String digits = el.getAsString().replaceAll("[^0-9-]", "");
                if (digits.isEmpty()) return null;
                f = Integer.parseInt(digits);
            }
            if (f == 0 || f < -2 || f > 15) return null;
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(JsonElement el, LocalDate today) {
        if (el == null || el.isJsonNull()) return null;
        try {
            String str = el.getAsString().trim();
            if (str.isEmpty() || "null".equalsIgnoreCase(str)) return null;
            LocalDate parsed = LocalDate.parse(str);
            return parsed.isAfter(today) ? null : parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringOrDefault(JsonObject obj, String key, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String val = obj.get(key).getAsString().trim();
        return val.isEmpty() ? fallback : val;
    }
}