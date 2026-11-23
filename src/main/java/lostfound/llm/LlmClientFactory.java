package lostfound.llm;

public final class LlmClientFactory {
    private LlmClientFactory() {}

    public static LlmClient create() {
        String key = System.getenv("LLM_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("[LLM] API 키가 없어 오프라인 모드로 실행합니다.");
            return new FakeLlmClient();
        }
        HttpLlmClient client = new HttpLlmClient(key);
        System.err.println("[LLM] 온라인 모드 활성화 (" + client.providerName() + " API 연결 완료)");
        return client;
    }
}