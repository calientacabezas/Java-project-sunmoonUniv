package lostfound.matching;

import lostfound.domain.Building;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static lostfound.TestItems.buds;
import static org.junit.jupiter.api.Assertions.*;

class LlmSemanticReRankerTest {

    /** 정해진 응답을 돌려주고, 받은 요청 내용을 기록하는 테스트용 클라이언트. */
    static final class ScriptedLlm implements LlmClient {
        final String response;
        final List<String> received = new ArrayList<>();
        ScriptedLlm(String response) { this.response = response; }
        @Override public String complete(String systemPrompt, String userInput) {
            received.add(userInput);
            if (response == null) throw new LlmException("네트워크 장애 흉내");
            return response;
        }
        @Override public boolean isOnline() { return true; }
    }

    private static MatchResult result(String id, int ruleScore) {
        Item item = buds(id, ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 22));
        return new MatchResult(item, ruleScore, 0, ruleScore, List.of(), "규칙 근거");
    }

    @Test void 최종_점수는_규칙_80퍼센트_의미_20퍼센트이고_다시_정렬된다() {
        ScriptedLlm llm = new ScriptedLlm("""
                {"results":[{"id":"A","score":0,"reason":"다른 물건"},{"id":"B","score":100,"reason":"버즈=무선 이어폰"}]}
                """);
        List<MatchResult> out = new LlmSemanticReRanker(llm).rerank("하얀 버즈", List.of(result("A", 80), result("B", 75)));

        assertEquals("B", out.get(0).item().getId());            // 75*0.8+100*0.2 = 80 > 80*0.8+0 = 64
        assertEquals(80, out.get(0).finalScore());
        assertEquals(64, out.get(1).finalScore());
        assertEquals(100, out.get(0).semanticScore());
        assertTrue(out.get(0).reason().contains("의미 보정"));
        assertEquals(1, llm.received.size(), "검색 1회당 API는 1번만 호출해야 함");
    }

    @Test void 코드블록으로_감싼_응답과_범위_밖_점수도_처리한다() {
        ScriptedLlm llm = new ScriptedLlm("```json\n{\"results\":[{\"id\":\"A\",\"score\":150}]}\n```");
        List<MatchResult> out = new LlmSemanticReRanker(llm).rerank("x", List.of(result("A", 50)));
        assertEquals(100, out.get(0).semanticScore());
    }

    @Test void 점수가_없는_후보는_규칙_점수를_그대로_유지한다() {
        ScriptedLlm llm = new ScriptedLlm("{\"results\":[{\"id\":\"A\",\"score\":90}]}");
        List<MatchResult> out = new LlmSemanticReRanker(llm).rerank("x", List.of(result("A", 50), result("B", 60)));
        MatchResult b = out.stream().filter(r -> r.item().getId().equals("B")).findFirst().orElseThrow();
        assertEquals(60, b.finalScore());
    }

    @Test void API_장애나_잘못된_응답이면_원래_목록을_그대로_돌려준다() {
        List<MatchResult> input = List.of(result("A", 80), result("B", 70));
        assertSame(input, new LlmSemanticReRanker(new ScriptedLlm(null)).rerank("x", input));
        assertSame(input, new LlmSemanticReRanker(new ScriptedLlm("JSON 아님")).rerank("x", input));
        assertSame(input, new LlmSemanticReRanker(new ScriptedLlm("{\"results\":[]}")).rerank("x", input));
    }

    @Test void 비공개_특징은_LLM에_보내지_않고_개인정보는_마스킹한다() {
        ScriptedLlm llm = new ScriptedLlm("{\"results\":[]}");
        new LlmSemanticReRanker(llm).rerank("연락처 010-1234-5678 하얀 버즈", List.of(result("A", 80)));
        String sent = llm.received.get(0);
        assertFalse(sent.contains("강아지 스티커"), "secret이 전송되면 안 됨");
        assertFalse(sent.contains("010-1234-5678"));
        assertTrue(sent.contains("[MASKED]"));
    }

    @Test void 후보가_없으면_API를_호출하지_않는다() {
        ScriptedLlm llm = new ScriptedLlm("{}");
        assertTrue(new LlmSemanticReRanker(llm).rerank("x", List.of()).isEmpty());
        assertTrue(llm.received.isEmpty());
    }
}
