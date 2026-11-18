package lostfound.llm;

/** 환경변수 LLM_API_KEY가 있으면 실제 클라이언트, 없으면 FakeLlmClient. */
public final class LlmClientFactory {
    private LlmClientFactory() {}

    public static LlmClient create() {
        String key = System.getenv("LLM_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("[LLM] API 키가 없어 오프라인 모드로 실행합니다.");
            return new FakeLlmClient();
        }
        // TODO(김동민): 제공사 확정 후 실제 클라이언트 반환. 키 값은 로그에 출력하지 않는다.
        System.err.println("[LLM] 실제 클라이언트 미구현: 오프라인 모드로 실행합니다.");
        return new FakeLlmClient();
    }
}

