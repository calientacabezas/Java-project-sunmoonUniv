package lostfound.llm;

/**
 * LLM 호출 계약. 구현체에 전달되는 userInput은 반드시 PiiMasker를 거친 문자열이어야 한다.
 * 담당: 김동민 (11/16 복귀 후 실제 구현).
 */
public interface LlmClient {
    String complete(String systemPrompt, String userInput) throws LlmException;
    boolean isOnline();
}
