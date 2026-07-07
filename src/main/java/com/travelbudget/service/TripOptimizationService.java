package com.travelbudget.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.client.AiClient;
import com.travelbudget.client.AiTripPlan;
import com.travelbudget.client.TripOptimizationRequest;
import com.travelbudget.dto.response.TripResponse;
import com.travelbudget.entity.Expense;
import com.travelbudget.entity.Trip;
import com.travelbudget.entity.TripDay;
import com.travelbudget.entity.TripStatus;
import com.travelbudget.exception.AiIntegrationException;
import com.travelbudget.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Оркестрация AI-оптимизации маршрута:
 *   1. берём поездку (с проверкой владельца — через TripService, без дублирования);
 *   2. просим AiClient построить план;
 *   3. парсим JSON-ответ в AiTripPlan;
 *   4. превращаем план в сущности TripDay + Expense и сохраняем (каскадом от Trip);
 *   5. переводим статус в OPTIMIZED.
 */
@Service
@RequiredArgsConstructor
public class TripOptimizationService {

    private final AiClient aiClient;
    private final TripService tripService;
    private final TripRepository tripRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TripResponse optimizeTrip(Long tripId, String userEmail) {
        // Проверка "твоя ли поездка" переиспользуется из TripService.
        Trip trip = tripService.getOwnedTripEntity(tripId, userEmail);

        // Интересы хранятся строкой "BEACH,MUSEUMS" -> превращаем в список.
        List<String> interests = (trip.getInterests() == null || trip.getInterests().isBlank())
                ? List.of()
                : Arrays.asList(trip.getInterests().split(","));

        TripOptimizationRequest request = new TripOptimizationRequest(
                trip.getOriginCity(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getBudget(),
                trip.getCurrency(),
                trip.getHotelStars(),
                trip.getAccommodationPreference(),
                trip.getFoodStyle(),
                interests
        );

        String rawResponse = aiClient.optimizeTrip(request);
        AiTripPlan plan = parsePlan(rawResponse);

        // Повторная оптимизация: убираем старые дни. orphanRemoval=true удалит их из БД.
        trip.getDays().clear();

        if (plan.days() != null) {
            for (AiTripPlan.Day dayPlan : plan.days()) {
                TripDay day = new TripDay();
                day.setTrip(trip);
                day.setDate(dayPlan.date());
                day.setCity(dayPlan.city());
                day.setDescription(dayPlan.description());

                if (dayPlan.expenses() != null) {
                    for (AiTripPlan.Expense expPlan : dayPlan.expenses()) {
                        Expense expense = new Expense();
                        expense.setTripDay(day);
                        expense.setCategory(expPlan.category());
                        expense.setDescription(expPlan.description());
                        expense.setEstimatedCost(expPlan.cost());
                        day.getExpenses().add(expense);
                    }
                }
                trip.getDays().add(day);
            }
        }

        trip.setStatus(TripStatus.OPTIMIZED);
        tripRepository.save(trip); // cascade ALL сохранит дни и расходы

        // Возвращаем свежее DTO (статус уже OPTIMIZED).
        return tripService.getTrip(tripId, userEmail);
    }

    /**
     * Парсим JSON от AI. Модель иногда оборачивает ответ в markdown-заборы ```json ... ```,
     * поэтому чистим их перед разбором — это частая практическая деталь.
     */
    private AiTripPlan parsePlan(String raw) {
        String json = stripCodeFences(raw);
        try {
            return objectMapper.readValue(json, AiTripPlan.class);
        } catch (JsonProcessingException e) {
            throw new AiIntegrationException("Не удалось разобрать ответ AI: " + e.getMessage());
        }
    }

    private String stripCodeFences(String raw) {
        String text = raw.strip();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1); // убрать строку "```json"
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.strip();
    }
}
