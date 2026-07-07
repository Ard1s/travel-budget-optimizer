package com.travelbudget.client;

import com.travelbudget.entity.AccommodationPreference;
import com.travelbudget.entity.FoodStyle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Вход для AiClient — всё, что нужно для построения маршрута, включая предпочтения.
 * Отделено от сущности Trip: клиенту незачем знать про JPA/пользователя.
 */
public record TripOptimizationRequest(
        String originCity,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String currency,
        Integer hotelStars,
        AccommodationPreference accommodationPreference,
        FoodStyle foodStyle,
        List<String> interests
) {
}
