package com.travelbudget.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.exception.AiIntegrationException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Клиент бесплатного Google Gemini API.
 *
 * Используем OkHttp (не стандартный java.net.http): у OkHttp "браузерный" TLS,
 * который проходит гео-фильтр Google. Стандартный java-клиент с того же IP стабильно
 * получал "User location is not supported", а OkHttp/curl — проходят.
 *
 * responseMimeType=application/json + thinkingBudget=0 -> чистый JSON без "размышлений".
 * Повтор при плавающей гео-ошибке на всякий случай.
 */
public class GeminiAiClient implements AiClient {

    private static final int MAX_TOKENS = 8192;
    private static final int MAX_ATTEMPTS = 4;
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiAiClient(String apiKey, String model, ObjectMapper objectMapper) {
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
        AiIntegrationException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callGemini(request);
            } catch (AiIntegrationException e) {
                last = e;
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (msg.contains("location is not supported") && attempt < MAX_ATTEMPTS) {
                    sleep(400);
                    continue;
                }
                throw e;
            }
        }
        throw last;
    }

    private String callGemini(TripOptimizationRequest request) {
        String url = BASE_URL + model + ":generateContent";

        GeminiRequest bodyObj = new GeminiRequest(
                List.of(new Content(List.of(new Part(TripPromptBuilder.build(request))))),
                new GenerationConfig("application/json", MAX_TOKENS, new ThinkingConfig(0))
        );

        try {
            String requestJson = objectMapper.writeValueAsString(bodyObj);
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("x-goog-api-key", apiKey)
                    .post(RequestBody.create(requestJson, JSON))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new AiIntegrationException("Gemini " + response.code() + ": " + responseBody);
                }

                GeminiResponse parsed = objectMapper.readValue(responseBody, GeminiResponse.class);
                if (parsed.candidates() == null || parsed.candidates().isEmpty()) {
                    throw new AiIntegrationException("Пустой ответ от Gemini");
                }
                Content content = parsed.candidates().get(0).content();
                if (content == null || content.parts() == null || content.parts().isEmpty()) {
                    throw new AiIntegrationException("Пустой ответ от Gemini");
                }

                String json = content.parts().stream()
                        .map(Part::text)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining());

                if (json.isBlank()) {
                    throw new AiIntegrationException("Пустой ответ от Gemini");
                }
                return json;
            }
        } catch (AiIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiIntegrationException("Ошибка вызова Gemini: " + e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Формат запроса/ответа Gemini (имена полей = ключи JSON) ---

    public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }

    public record GenerationConfig(String responseMimeType, int maxOutputTokens, ThinkingConfig thinkingConfig) {
    }

    public record ThinkingConfig(int thinkingBudget) {
    }

    public record GeminiResponse(List<Candidate> candidates) {
    }

    public record Candidate(Content content) {
    }
}
