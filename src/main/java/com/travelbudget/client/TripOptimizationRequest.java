package com.travelbudget.client;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Вход для AiClient — то, что нужно AI для построения маршрута.
 * Отделено от сущности Trip: клиенту незачем знать про JPA/пользователя.
 */
public record TripOptimizationRequest(
        String originCity,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String currency
) {
}
