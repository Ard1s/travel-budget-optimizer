package com.travelbudget.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.exception.AiIntegrationException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.time.Duration;
import java.util.List;

/**
 * Клиент бесплатного Groq API (OpenAI-совместимый).
 * Щедрые лимиты, без карты, без гео-ограничений. Модель по умолчанию — Llama 3.3 70B.
 *
 * response_format=json_object -> модель возвращает чистый JSON (наш промпт просит JSON).
 */
public class GroqAiClient implements AiClient {

    private static final int MAX_TOKENS = 8192;
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GroqAiClient(String apiKey, String model, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        GroqRequest bodyObj = new GroqRequest(
                model,
                List.of(new Message("user", TripPromptBuilder.build(request))),
                MAX_TOKENS,
                new ResponseFormat("json_object")
        );

        try {
            String requestJson = objectMapper.writeValueAsString(bodyObj);
            Request httpRequest = new Request.Builder()
                    .url(URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(requestJson, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new AiIntegrationException("Groq " + response.code() + ": " + responseBody);
                }

                GroqResponse parsed = objectMapper.readValue(responseBody, GroqResponse.class);
                if (parsed.choices() == null || parsed.choices().isEmpty()
                        || parsed.choices().get(0).message() == null) {
                    throw new AiIntegrationException("Пустой ответ от Groq");
                }
                String json = parsed.choices().get(0).message().content();
                if (json == null || json.isBlank()) {
                    throw new AiIntegrationException("Пустой ответ от Groq");
                }
                return json;
            }
        } catch (AiIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiIntegrationException("Ошибка вызова Groq: " + e.getMessage());
        }
    }

    // --- Формат запроса/ответа (OpenAI-совместимый) ---

    public record GroqRequest(
            String model,
            List<Message> messages,
            @JsonProperty("max_tokens") int maxTokens,
            @JsonProperty("response_format") ResponseFormat responseFormat) {
    }

    public record Message(String role, String content) {
    }

    public record ResponseFormat(String type) {
    }

    public record GroqResponse(List<Choice> choices) {
    }

    public record Choice(Message message) {
    }
}
