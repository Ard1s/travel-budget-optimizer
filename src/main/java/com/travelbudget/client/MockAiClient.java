package com.travelbudget.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.entity.ExpenseCategory;
import com.travelbudget.exception.AiIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Заглушка AI для профиля "local": строит план БЕЗ похода в сеть.
 *
 * ВАЖНО: числа НЕ захардкожены — план распределяет реальный бюджет пользователя
 * по категориям (в процентах) и по фактическому числу дней поездки. Поэтому суммы
 * зависят и от бюджета, и от валюты (считаем прямо от введённой суммы), и от дат.
 * Совокупно план "съедает" ~92% бюджета, остаток — резерв.
 *
 * Это по-прежнему демо: реального анализа направления/сезона тут нет — за этим
 * нужен настоящий Claude (AnthropicAiClient, профиль по умолчанию + AI_API_KEY).
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    // Доли бюджета по категориям (в сумме ~0.92, остальное — резерв).
    private static final double FLIGHTS_SHARE = 0.28;
    private static final double HOTELS_SHARE = 0.34;
    private static final double FOOD_SHARE = 0.18;
    private static final double TRANSPORT_SHARE = 0.06;
    private static final double ACTIVITIES_SHARE = 0.06;

    private static final int MAX_DAYS = 14; // ограничиваем размер плана для демо

    private final ObjectMapper objectMapper;

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        BigDecimal budget = request.budget() != null ? request.budget() : new BigDecimal("1000");

        // Число дней поездки (включительно). Если дат нет — 3 дня по умолчанию.
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now();
        LocalDate end = request.endDate();
        int days = 3;
        if (request.startDate() != null && end != null && !end.isBefore(start)) {
            long span = ChronoUnit.DAYS.between(start, end) + 1;
            days = (int) Math.min(span, MAX_DAYS);
        }
        if (days < 1) {
            days = 1;
        }

        // Итоговые суммы по категориям от бюджета.
        BigDecimal flightsTotal = share(budget, FLIGHTS_SHARE);
        BigDecimal hotelsTotal = share(budget, HOTELS_SHARE);
        BigDecimal foodTotal = share(budget, FOOD_SHARE);
        BigDecimal transportTotal = share(budget, TRANSPORT_SHARE);
        BigDecimal activitiesTotal = share(budget, ACTIVITIES_SHARE);

        // Раскидываем по дням (перелёт — разово в первый день).
        BigDecimal hotelPerDay = perDay(hotelsTotal, days);
        BigDecimal foodPerDay = perDay(foodTotal, days);
        BigDecimal transportPerDay = perDay(transportTotal, days);
        BigDecimal activitiesPerDay = perDay(activitiesTotal, days);

        List<AiTripPlan.Day> dayPlans = new ArrayList<>();
        LocalDate date = start;
        for (int i = 0; i < days; i++) {
            List<AiTripPlan.Expense> expenses = new ArrayList<>();

            if (i == 0) {
                expenses.add(new AiTripPlan.Expense(ExpenseCategory.FLIGHT,
                        "Перелёт " + request.originCity() + " → " + request.destination(), flightsTotal));
            }
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.HOTEL,
                    "Проживание, ночь " + (i + 1), hotelPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.FOOD,
                    "Питание", foodPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.TRANSPORT,
                    "Транспорт по городу", transportPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.ACTIVITIES,
                    "Экскурсии и развлечения", activitiesPerDay));

            dayPlans.add(new AiTripPlan.Day(date, request.destination(),
                    "День " + (i + 1) + " в " + request.destination(), expenses));
            date = date.plusDays(1);
        }

        // budgetBreakdown с фактически распределёнными по дням суммами (для консистентности).
        AiTripPlan.Breakdown breakdown = new AiTripPlan.Breakdown(
                flightsTotal,
                hotelPerDay.multiply(BigDecimal.valueOf(days)),
                foodPerDay.multiply(BigDecimal.valueOf(days)),
                transportPerDay.multiply(BigDecimal.valueOf(days)),
                activitiesPerDay.multiply(BigDecimal.valueOf(days))
        );
        BigDecimal total = breakdown.flights()
                .add(breakdown.hotels())
                .add(breakdown.food())
                .add(breakdown.transport())
                .add(breakdown.activities());

        AiTripPlan plan = new AiTripPlan(
                dayPlans,
                total,
                breakdown,
                List.of(
                        "Бронируй жильё заранее — так дешевле",
                        "Проездной на транспорт выгоднее разовых билетов",
                        "Обед по меню дня экономит на еде"
                )
        );

        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException("Не удалось собрать mock-ответ: " + e.getMessage());
        }
    }

    /** Доля бюджета, округлённая до копеек. */
    private BigDecimal share(BigDecimal budget, double fraction) {
        return budget.multiply(BigDecimal.valueOf(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Сумма на один день. */
    private BigDecimal perDay(BigDecimal total, int days) {
        return total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }
}
