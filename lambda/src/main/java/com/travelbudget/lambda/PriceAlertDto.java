package com.travelbudget.lambda;

import java.math.BigDecimal;

/**
 * DTO подписки на цену — то, что Lambda получает из нашего API.
 *
 * Это ОТДЕЛЬНЫЙ класс от сущности PriceAlert в основном приложении:
 * Lambda и API — независимые сервисы, они общаются по JSON-контракту, а не по коду.
 * targetDate держим строкой — Lambda её не парсит, значит не тянем модуль java-time в Jackson.
 */
public record PriceAlertDto(
        Long id,
        String origin,
        String destination,
        String targetDate,
        BigDecimal targetPrice,
        String currency,
        boolean active
) {
}
