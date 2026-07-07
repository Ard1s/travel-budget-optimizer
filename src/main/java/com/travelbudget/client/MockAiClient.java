package com.travelbudget.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.entity.AccommodationPreference;
import com.travelbudget.entity.ExpenseCategory;
import com.travelbudget.entity.FoodStyle;
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
 * Заглушка AI для профиля "local": строит план БЕЗ похода в сеть, но теперь
 * УЧИТЫВАЕТ предпочтения (звёзды, расположение, стиль еды, интересы).
 *
 * Логика: базовые доли бюджета по категориям умножаются на коэффициенты,
 * зависящие от выбора пользователя. Поэтому «5★ у моря + рестораны» может дать
 * перерасход (remaining < 0), а «хостел + магазины» оставит запас — это уже осмысленно.
 *
 * Это по-прежнему эвристика, НЕ реальные цены. За реальными нужны Claude (план)
 * и Aviasales/Hotellook (цены) — фазы 2 и 3.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private static final int MAX_DAYS = 14;

    // Базовые доли бюджета (до применения коэффициентов предпочтений).
    private static final double BASE_FLIGHTS = 0.30;
    private static final double BASE_HOTEL = 0.30;
    private static final double BASE_FOOD = 0.15;
    private static final double BASE_TRANSPORT = 0.05;
    private static final double BASE_ACTIVITIES = 0.05;

    private final ObjectMapper objectMapper;

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        BigDecimal budget = request.budget() != null ? request.budget() : new BigDecimal("1000");

        // --- число дней ---
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now();
        LocalDate end = request.endDate();
        int days = 3;
        if (request.startDate() != null && end != null && !end.isBefore(start)) {
            days = (int) Math.min(ChronoUnit.DAYS.between(start, end) + 1, MAX_DAYS);
        }
        if (days < 1) {
            days = 1;
        }

        // --- предпочтения с умолчаниями ---
        int stars = request.hotelStars() != null ? request.hotelStars() : 3;
        AccommodationPreference acc = request.accommodationPreference() != null
                ? request.accommodationPreference() : AccommodationPreference.ANY;
        FoodStyle food = request.foodStyle() != null ? request.foodStyle() : FoodStyle.MIXED;
        int interestCount = request.interests() != null ? request.interests().size() : 0;

        // --- коэффициенты от предпочтений ---
        double hotelFactor = switch (stars) {
            case 1 -> 0.5;
            case 2 -> 0.75;
            case 4 -> 1.4;
            case 5 -> 1.9;
            default -> 1.0; // 3 звезды
        };
        hotelFactor *= switch (acc) {
            case NEAR_SEA -> 1.15;    // жильё у моря дороже
            case CITY_CENTER -> 1.08; // центр чуть дороже
            default -> 1.0;
        };
        double foodFactor = switch (food) {
            case SELF_CATERING -> 0.5;
            case CAFES -> 1.0;
            case RESTAURANTS -> 1.9;
            case MIXED -> 1.2;
        };
        double activitiesFactor = Math.min(1.0 + 0.2 * interestCount, 2.0);

        // --- итоговые суммы по категориям ---
        BigDecimal flightsTotal = share(budget, BASE_FLIGHTS);
        BigDecimal hotelsTotal = share(budget, BASE_HOTEL * hotelFactor);
        BigDecimal foodTotal = share(budget, BASE_FOOD * foodFactor);
        BigDecimal transportTotal = share(budget, BASE_TRANSPORT);
        BigDecimal activitiesTotal = share(budget, BASE_ACTIVITIES * activitiesFactor);

        BigDecimal hotelPerDay = perDay(hotelsTotal, days);
        BigDecimal foodPerDay = perDay(foodTotal, days);
        BigDecimal transportPerDay = perDay(transportTotal, days);
        BigDecimal activitiesPerDay = perDay(activitiesTotal, days);

        String hotelLabel = "Отель " + "★".repeat(stars) + accLabel(acc);
        String foodLabel = "Питание — " + foodLabel(food);

        List<AiTripPlan.Day> dayPlans = new ArrayList<>();
        LocalDate date = start;
        for (int i = 0; i < days; i++) {
            List<AiTripPlan.Expense> expenses = new ArrayList<>();
            if (i == 0) {
                expenses.add(new AiTripPlan.Expense(ExpenseCategory.FLIGHT,
                        "Перелёт " + request.originCity() + " → " + request.destination(), flightsTotal));
            }
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.HOTEL,
                    hotelLabel + ", ночь " + (i + 1), hotelPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.FOOD, foodLabel, foodPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.TRANSPORT, "Транспорт по городу", transportPerDay));
            expenses.add(new AiTripPlan.Expense(ExpenseCategory.ACTIVITIES,
                    "Экскурсии и развлечения", activitiesPerDay));

            dayPlans.add(new AiTripPlan.Day(date, request.destination(),
                    "День " + (i + 1) + " в " + request.destination(), expenses));
            date = date.plusDays(1);
        }

        AiTripPlan.Breakdown breakdown = new AiTripPlan.Breakdown(
                flightsTotal,
                hotelPerDay.multiply(BigDecimal.valueOf(days)),
                foodPerDay.multiply(BigDecimal.valueOf(days)),
                transportPerDay.multiply(BigDecimal.valueOf(days)),
                activitiesPerDay.multiply(BigDecimal.valueOf(days))
        );
        BigDecimal total = breakdown.flights().add(breakdown.hotels()).add(breakdown.food())
                .add(breakdown.transport()).add(breakdown.activities());

        AiTripPlan plan = new AiTripPlan(dayPlans, total, breakdown, List.of(
                "Бронируй жильё заранее — так дешевле",
                "Проездной на транспорт выгоднее разовых билетов",
                food == FoodStyle.RESTAURANTS ? "Обед по меню дня дешевле ужина в ресторане"
                        : "Локальные рынки — вкусно и недорого"
        ));

        try {
            return objectMapper.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException("Не удалось собрать mock-ответ: " + e.getMessage());
        }
    }

    private BigDecimal share(BigDecimal budget, double fraction) {
        return budget.multiply(BigDecimal.valueOf(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal perDay(BigDecimal total, int days) {
        return total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private String accLabel(AccommodationPreference acc) {
        return switch (acc) {
            case NEAR_SEA -> " у моря";
            case CITY_CENTER -> " в центре";
            case QUIET -> " в тихом районе";
            case ANY -> "";
        };
    }

    private String foodLabel(FoodStyle food) {
        return switch (food) {
            case SELF_CATERING -> "магазины/самостоятельно";
            case CAFES -> "кафе и стрит-фуд";
            case RESTAURANTS -> "рестораны";
            case MIXED -> "смешанно";
        };
    }
}
