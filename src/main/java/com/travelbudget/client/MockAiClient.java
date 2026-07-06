package com.travelbudget.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.entity.ExpenseCategory;
import com.travelbudget.exception.AiIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Заглушка AI для профиля "local": возвращает заранее собранный план БЕЗ похода в сеть.
 * Нужна, чтобы разрабатывать и проверять поток optimize локально без ключа и без затрат.
 *
 * Собираем настоящий AiTripPlan и сериализуем его в JSON-строку — так же, как вернул бы
 * реальный клиент. Это заодно "прогоняет" наши DTO через Jackson в обе стороны.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private final ObjectMapper objectMapper;

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        List<AiTripPlan.Day> days = new ArrayList<>();

        BigDecimal flights = new BigDecimal("120.00");
        BigDecimal hotelPerNight = new BigDecimal("45.00");
        BigDecimal foodPerDay = new BigDecimal("25.00");

        LocalDate date = request.startDate();
        int i = 0;
        // Для заглушки достаточно до 3 дней.
        while (date != null && request.endDate() != null
                && !date.isAfter(request.endDate()) && i < 3) {

            List<AiTripPlan.Expense> expenses = new ArrayList<>();
            if (i == 0) {
                expenses.add(new AiTripPlan.Expense(ExpenseCategory.FLIGHT,
                        "Перелёт " + request.originCity() + " → " + request.destination(), flights));
            }
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.HOTEL,
                    "Отель, ночь " + (i + 1), hotelPerNight));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.FOOD,
                    "Питание, день " + (i + 1), foodPerDay));

            days.add(new AiTripPlan.Day(date, request.destination(),
                    "День " + (i + 1) + ": прогулка и осмотр достопримечательностей", expenses));

            date = date.plusDays(1);
            i++;
        }

        int nights = days.size();
        AiTripPlan.Breakdown breakdown = new AiTripPlan.Breakdown(
                flights,
                hotelPerNight.multiply(BigDecimal.valueOf(nights)),
                foodPerDay.multiply(BigDecimal.valueOf(nights)),
                new BigDecimal("0.00"),
                new BigDecimal("0.00")
        );
        BigDecimal total = breakdown.flights()
                .add(breakdown.hotels())
                .add(breakdown.food());

        AiTripPlan plan = new AiTripPlan(
                days,
                total,
                breakdown,
                List.of("Покупай билеты в музеи онлайн", "Меню дня в ресторанах дешевле")
        );

        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException("Не удалось собрать mock-ответ: " + e.getMessage());
        }
    }
}
