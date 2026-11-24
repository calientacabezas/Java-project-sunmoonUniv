package lostfound.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 선문대학교 발급 LLM REST API 호출 클라이언트
 */
public class HttpLlmClient implements LlmClient {

    // =========================================================================
    // ★★★ [필수 설정] 선문대학교에서 발급받은 API 정보를 아래에 입력하세요 ★★★
    // =========================================================================
    private static final String SUNMOON_API_KEY = "";

    // 학교 안내 문서에 별도 서버 주소(Endpoint)가 있다면 아래 주소를 수정하세요.
    // (단순 OpenAI 호환 키만 발급받은 경우 기본값 그대로 사용하면 됩니다.)
    private static final String SUNMOON_API_URL = "";

    // 학교 안내 문서에 지정된 모델명이 있다면 수정하세요. (예: gpt-4o-mini, gpt-4o, llama3 등)
    private static final String SUNMOON_MODEL = "gemma-4-26b";
    // =========================================================================

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    /**
     * 코드 상단의 SUNMOON_API_KEY를 기본으로 사용하는 생성자
     */
    public HttpLlmClient() {
        this(SUNMOON_API_KEY);
    }

    /**
     * 외부(Main 등)에서 키를 넘겨받을 때도 동작하는 생성자
     * (외부 값이 비어있으면 상단의 SUNMOON_API_KEY를 자동으로 사용)
     */
    public HttpLlmClient(String apiKey) {
        String resolvedKey = (apiKey != null && !apiKey.isBlank() && !apiKey.contains("여기에_발급받은"))
                ? apiKey
                : SUNMOON_API_KEY;

        // 양옆 공백 및 실수로 들어간 큰따옴표("), 작은따옴표(') 자동 제거
        this.apiKey = resolvedKey.trim().replaceAll("^[\"']+|[\"']+$", "");

        String envUrl = System.getenv("LLM_API_URL");
        this.apiUrl = (envUrl != null && !envUrl.isBlank()) ? envUrl.trim() : SUNMOON_API_URL;

        String envModel = System.getenv("LLM_MODEL");
        this.model = (envModel != null && !envModel.isBlank()) ? envModel.trim() : SUNMOON_MODEL;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userInput) throws LlmException {
        if (apiKey.isBlank() || apiKey.contains("여기에_발급받은")) {
            throw new LlmException("API 키가 설정되지 않았습니다. HttpLlmClient.java 상단의 SUNMOON_API_KEY를 입력해주세요.");
        }

        try {
            return callSunMoonApi(systemPrompt, userInput);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("선문대 LLM API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String callSunMoonApi(String systemPrompt, String userInput) throws Exception {
        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userInput);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() / 100 != 2) {
            throw new LlmException("API 응답 오류 (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

        // 표준 OpenAI 호환 응답 파싱 (choices[0].message.content)
        if (root.has("choices") && root.getAsJsonArray("choices").size() > 0) {
            return root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }

        throw new LlmException("예상치 못한 응답 형식입니다. 응답 원문: " + response.body());
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    public String providerName() {
        return "SunMoon-LLM (" + model + ")";
    }
}