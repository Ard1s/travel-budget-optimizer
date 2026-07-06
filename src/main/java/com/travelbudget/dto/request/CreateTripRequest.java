package com.travelbudget.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Тело POST /api/trips — создание новой поездки.
 */
public record CreateTripRequest(

        @NotBlank(message = "Пункт назначения обязателен")
        String destination,

        @NotBlank(message = "Город отправления обязателен")
        String originCity,

        @NotNull(message = "Дата начала обязательна")
        LocalDate startDate,

        @NotNull(message = "Дата окончания обязательна")
        LocalDate endDate,

        @NotNull(message = "Бюджет обязателен")
        @DecimalMin(value = "0.01", message = "Бюджет должен быть больше нуля")
        BigDecimal budget,

        @NotBlank(message = "Валюта обязательна")
        @Size(min = 3, max = 3, message = "Валюта — 3-буквенный код, например EUR")
        String currency
) {

    /**
     * Кросс-полевая проверка: дата окончания не раньше даты начала.
     * Bean Validation воспринимает boolean-метод isXxx() как свойство "datesOrdered"
     * и применяет к нему @AssertTrue. Если одна из дат null — пропускаем
     * (за это отвечают отдельные @NotNull выше).
     */
    @AssertTrue(message = "Дата окончания не может быть раньше даты начала")
    public boolean isDatesOrdered() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
