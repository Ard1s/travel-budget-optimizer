package com.travelbudget.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.travelbudget.entity.AccommodationPreference;
import com.travelbudget.entity.FoodStyle;
import com.travelbudget.exception.AiIntegrationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реальный клиент Claude через официальный Java SDK Anthropic.
 *
 * Создаётся в AiClientConfig ТОЛЬКО когда задан ключ (ai.api.key). Клиент SDK
 * строим один раз в конструкторе — он потокобезопасный и переиспользуется.
 *
 * Мышление (thinking) НЕ включаем: для генерации JSON-плана это лишняя латентность
 * и расходы; Opus 4.8 без thinking отвечает быстро и следует инструкции про "только JSON".
 */
public class AnthropicAiClient implements AiClient {

    private static final long MAX_TOKENS = 8192L;

    private final AnthropicClient client;
    private final String model;

    public AnthropicAiClient(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(buildPrompt(request))
                .build();

        try {
            Message response = client.messages().create(params);

            // Ответ — список блоков; берём текстовые и склеиваем (это и есть JSON-план).
            String json = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining());

            if (json.isBlank()) {
                throw new AiIntegrationException("Пустой ответ от Claude");
            }
            return json;

        } catch (AiIntegrationException e) {
            throw e;
        } catch (Exception e) {
            // 401 (плохой ключ), сеть, лимиты и т.п. -> единый AiIntegrationException (обработается -> 502).
            throw new AiIntegrationException("Ошибка вызова Claude: " + e.getMessage());
        }
    }

    private String buildPrompt(TripOptimizationRequest r) {
        return """
                Ты — опытный трэвел-планировщик. Составь реалистичный план поездки, укладывающийся в бюджет.

                Откуда: %s
                Куда: %s
                Даты: %s — %s
                Бюджет: %s %s
                Отель: %s
                Питание: %s
                Интересы: %s

                Требования:
                - Уложись в бюджет (%s %s) и распредели расходы по дням.
                - Цены реалистичны для этого направления и дат; учитывай звёзды отеля и стиль питания.
                - Для КАЖДОГО дня в поле description опиши, что посмотреть: пометь [must-see] обязательное
                  и что можно успеть по времени. Учитывай интересы путешественника.
                - В tips добавь 2-4 совета по экономии и главные must-see всей поездки.
                - Все тексты (description, tips) — на русском языке.

                Верни ТОЛЬКО валидный JSON, без markdown и без пояснений, строго по схеме:
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
                r.budget(), r.currency(),
                hotelLabel(r.hotelStars(), r.accommodationPreference()),
                foodLabel(r.foodStyle()),
                interestsLabel(r.interests()),
                r.budget(), r.currency()
        );
    }

    private String hotelLabel(Integer stars, AccommodationPreference acc) {
        String s = (stars != null) ? stars + "★" : "любые звёзды";
        String place = switch (acc != null ? acc : AccommodationPreference.ANY) {
            case NEAR_SEA -> ", у моря";
            case CITY_CENTER -> ", в центре";
            case QUIET -> ", тихий район";
            case ANY -> "";
        };
        return s + place;
    }

    private String foodLabel(FoodStyle food) {
        return switch (food != null ? food : FoodStyle.MIXED) {
            case SELF_CATERING -> "магазины/самостоятельно";
            case CAFES -> "кафе и стрит-фуд";
            case RESTAURANTS -> "рестораны";
            case MIXED -> "смешанно";
        };
    }

    private String interestsLabel(List<String> interests) {
        return (interests == null || interests.isEmpty()) ? "не указаны" : String.join(", ", interests);
    }
}
