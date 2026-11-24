package lostfound.llm;

/**
 * LLM 호출 계약. 구현체에 전달되는 userInput은 반드시 PiiMasker를 거친 문자열이어야 한다.
 */
public interface LlmClient {
    String complete(String systemPrompt, String userInput) throws LlmException;
    boolean isOnline();
}

