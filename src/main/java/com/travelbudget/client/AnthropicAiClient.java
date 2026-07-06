package com.travelbudget.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelbudget.exception.AiIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Реальный клиент Claude API (https://api.anthropic.com/v1/messages).
 * Активен во всех профилях, КРОМЕ "local" (@Profile("!local")).
 */
@Component
@Profile("!local")
@RequiredArgsConstructor
public class AnthropicAiClient implements AiClient {

    private final RestClient aiRestClient;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.model}")
    private String model;

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiIntegrationException("AI-ключ не задан (переменная AI_API_KEY)");
        }

        AnthropicRequest body = new AnthropicRequest(
                model,
                1500,
                List.of(new Message("user", buildPrompt(request)))
        );

        AnthropicResponse response = aiRestClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                // Любой 4xx/5xx превращаем в наше исключение (обработается -> 502).
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiIntegrationException("AI API вернул статус " + res.getStatusCode());
                })
                .body(AnthropicResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new AiIntegrationException("Пустой ответ от AI API");
        }
        return response.content().get(0).text();
    }

    private String buildPrompt(TripOptimizationRequest r) {
        return """
                Ты эксперт по планированию путешествий. Построй план поездки по бюджету.

                Откуда: %s
                Куда: %s
                Даты: %s — %s
                Бюджет: %s %s

                Верни ТОЛЬКО валидный JSON (без пояснений, без markdown) со структурой:
                {
                  "days": [
                    {"date":"YYYY-MM-DD","city":"...","description":"...",
                     "expenses":[{"category":"FLIGHT|HOTEL|FOOD|TRANSPORT|ACTIVITIES|OTHER",
                                  "description":"...","cost":0.00}]}
                  ],
                  "totalEstimatedCost": 0.00,
                  "budgetBreakdown": {"flights":0.00,"hotels":0.00,"food":0.00,"transport":0.00,"activities":0.00},
                  "tips": ["...", "..."]
                }
                """.formatted(
                r.originCity(), r.destination(),
                r.startDate(), r.endDate(),
                r.budget(), r.currency()
        );
    }

    // --- Формат запроса/ответа Anthropic (вложенные, т.к. используются только здесь) ---

    public record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            List<Message> messages
    ) {
    }

    public record Message(String role, String content) {
    }

    public record AnthropicResponse(List<ContentBlock> content) {
    }

    public record ContentBlock(String type, String text) {
    }
}
