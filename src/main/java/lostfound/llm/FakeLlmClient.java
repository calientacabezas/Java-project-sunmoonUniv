package lostfound.llm;

import java.util.HashMap;
import java.util.Map;

/**
 * API 없이 개발·테스트할 때 쓰는 가짜 클라이언트.
 * 등록한 입력에는 등록한 응답을, 그 외에는 LlmException을 던져 대체 경로를 시험할 수 있다.
 */
public class FakeLlmClient implements LlmClient {
    private final Map<String, String> responses = new HashMap<>();

    public FakeLlmClient willReturn(String userInput, String response) {
        responses.put(userInput, response);
        return this;
    }

    @Override
    public String complete(String systemPrompt, String userInput) {
        String r = responses.get(userInput);
        if (r == null) throw new LlmException("오프라인 모드: 등록된 응답 없음");
        return r;
    }

    @Override
    public boolean isOnline() { return false; }
}
