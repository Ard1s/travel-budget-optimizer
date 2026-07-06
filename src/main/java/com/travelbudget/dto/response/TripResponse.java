package com.travelbudget.dto.response;

import com.travelbudget.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Исходящее представление поездки. Отдаём наружу вместо Entity (правило проекта №6):
 * не светим связи (user, days) и полностью контролируем формат ответа.
 * TripStatus Jackson сериализует как строку ("DRAFT"/"OPTIMIZED"/"CONFIRMED").
 */
public record TripResponse(
        Long id,
        String destination,
        String originCity,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String currency,
        TripStatus status,
        LocalDateTime createdAt
) {
}
