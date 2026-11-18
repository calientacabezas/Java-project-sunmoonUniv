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
 * 실제 LLM REST API 호출 클라이언트 (Gemini / OpenAI 키 자동 판별).
 */
public class HttpLlmClient implements LlmClient {

    private final String apiKey;
    private final boolean isGemini;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public HttpLlmClient(String apiKey) {
        // 양옆 공백 및 실수로 들어간 큰따옴표("), 작은따옴표(') 자동 제거
        this.apiKey = apiKey.trim().replaceAll("^[\"']+|[\"']+$", "");
        String providerEnv = System.getenv("LLM_PROVIDER");
        if (providerEnv != null && !providerEnv.isBlank()) {
            this.isGemini = providerEnv.trim().equalsIgnoreCase("gemini");
        } else {
            this.isGemini = this.apiKey.startsWith("AIza");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userInput) throws LlmException {
        try {
            if (isGemini) {
                return callGemini(systemPrompt, userInput);
            } else {
                return callOpenAi(systemPrompt, userInput);
            }
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String callGemini(String systemPrompt, String userInput) throws Exception {
        String model = System.getenv("LLM_MODEL");
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        JsonArray sysParts = new JsonArray();
        sysParts.add(sysPart);
        JsonObject sysInst = new JsonObject();
        sysInst.add("parts", sysParts);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userInput);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject content = new JsonObject();
        content.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.1);

        JsonObject body = new JsonObject();
        body.add("systemInstruction", sysInst);
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new LlmException("Gemini API 응답 오류 (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        return root.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }

    private String callOpenAi(String systemPrompt, String userInput) throws Exception {
        String model = System.getenv("LLM_MODEL");
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        JsonArray messages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userInput);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new LlmException("OpenAI API 응답 오류 (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    public String providerName() {
        return isGemini ? "Gemini" : "OpenAI";
    }
}
