package com.travelbudget.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Тело PATCH /api/trips/{id} — ЧАСТИЧНОЕ обновление.
 *
 * Все поля необязательные (nullable): обновляем только те, что пришли не-null.
 * Поэтому здесь нет @NotBlank/@NotNull. Ограничения @DecimalMin/@Size
 * срабатывают только если значение реально передано (для null — пропускаются).
 *
 * status тут не меняем специально: он управляется бизнес-логикой (AI-оптимизация в Модуле 5).
 */
public record UpdateTripRequest(

        String destination,

        String originCity,

        LocalDate startDate,

        LocalDate endDate,

        @DecimalMin(value = "0.01", message = "Бюджет должен быть больше нуля")
        BigDecimal budget,

        @Size(min = 3, max = 3, message = "Валюта — 3-буквенный код, например EUR")
        String currency
) {
}
