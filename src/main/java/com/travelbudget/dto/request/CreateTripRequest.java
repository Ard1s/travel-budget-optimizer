package com.travelbudget.dto.request;

import com.travelbudget.entity.AccommodationPreference;
import com.travelbudget.entity.FoodStyle;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Тело POST /api/trips — создание новой поездки.
 *
 * Поля предпочтений (hotelStars/accommodationPreference/foodStyle/interests)
 * необязательные: если не заданы, план строится по разумным умолчаниям.
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
        String currency,

        @Min(value = 1, message = "Звёзды отеля от 1 до 5")
        @Max(value = 5, message = "Звёзды отеля от 1 до 5")
        Integer hotelStars,

        AccommodationPreference accommodationPreference,

        FoodStyle foodStyle,

        List<String> interests
) {

    /**
     * Упрощённый конструктор без предпочтений (используется в тестах и старом коде).
     * Делегирует канонический, подставляя предпочтения как null.
     */
    public CreateTripRequest(String destination, String originCity, LocalDate startDate,
                             LocalDate endDate, BigDecimal budget, String currency) {
        this(destination, originCity, startDate, endDate, budget, currency, null, null, null, null);
    }

    /**
     * Кросс-полевая проверка: дата окончания не раньше даты начала.
     */
    @AssertTrue(message = "Дата окончания не может быть раньше даты начала")
    public boolean isDatesOrdered() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
