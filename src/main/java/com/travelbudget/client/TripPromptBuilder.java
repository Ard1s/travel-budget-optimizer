package com.travelbudget.client;

import com.travelbudget.entity.AccommodationPreference;
import com.travelbudget.entity.FoodStyle;

import java.util.List;

/**
 * Единый промпт для любой нейросети (Claude, Gemini, ...). Так план строится
 * одинаково независимо от провайдера, и логика не дублируется по клиентам.
 */
public final class TripPromptBuilder {

    private TripPromptBuilder() {
    }

    public static String build(TripOptimizationRequest r) {
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

    private static String hotelLabel(Integer stars, AccommodationPreference acc) {
        String s = (stars != null) ? stars + "★" : "любые звёзды";
        String place = switch (acc != null ? acc : AccommodationPreference.ANY) {
            case NEAR_SEA -> ", у моря";
            case CITY_CENTER -> ", в центре";
            case QUIET -> ", тихий район";
            case ANY -> "";
        };
        return s + place;
    }

    private static String foodLabel(FoodStyle food) {
        return switch (food != null ? food : FoodStyle.MIXED) {
            case SELF_CATERING -> "магазины/самостоятельно";
            case CAFES -> "кафе и стрит-фуд";
            case RESTAURANTS -> "рестораны";
            case MIXED -> "смешанно";
        };
    }

    private static String interestsLabel(List<String> interests) {
        return (interests == null || interests.isEmpty()) ? "не указаны" : String.join(", ", interests);
    }
}
