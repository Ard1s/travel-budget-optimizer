package com.travelbudget.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travelbudget.client.AiClient;
import com.travelbudget.dto.response.TripResponse;
import com.travelbudget.entity.ExpenseCategory;
import com.travelbudget.entity.Trip;
import com.travelbudget.entity.TripStatus;
import com.travelbudget.entity.User;
import com.travelbudget.exception.AiIntegrationException;
import com.travelbudget.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тест оптимизации: AiClient замокан, реальной сети/ключа нет.
 * Проверяем самое рискованное — парсинг JSON и построение сущностей.
 */
@ExtendWith(MockitoExtension.class)
class TripOptimizationServiceTest {

    @Mock private AiClient aiClient;
    @Mock private TripService tripService;
    @Mock private TripRepository tripRepository;

    private TripOptimizationService service;

    @BeforeEach
    void setUp() {
        // Реальный ObjectMapper с поддержкой LocalDate (как в бою у Spring Boot).
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new TripOptimizationService(aiClient, tripService, tripRepository, objectMapper);
    }

    private Trip sampleTrip() {
        User owner = new User();
        owner.setEmail("owner@test.com");
        Trip trip = new Trip();
        trip.setId(10L);
        trip.setUser(owner);
        trip.setOriginCity("Warsaw");
        trip.setDestination("Barcelona");
        trip.setStartDate(LocalDate.of(2025, 8, 1));
        trip.setEndDate(LocalDate.of(2025, 8, 2));
        trip.setBudget(new BigDecimal("1000.00"));
        trip.setCurrency("EUR");
        return trip;
    }

    @Test
    void optimize_parsesPlan_buildsDays_setsOptimized() {
        Trip trip = sampleTrip();
        when(tripService.getOwnedTripEntity(10L, "owner@test.com")).thenReturn(trip);
        String json = """
                {
                  "days": [
                    {"date":"2025-08-01","city":"Barcelona","description":"Day 1",
                     "expenses":[
                        {"category":"FLIGHT","description":"flight","cost":120.00},
                        {"category":"HOTEL","description":"hotel","cost":45.00}
                     ]}
                  ],
                  "totalEstimatedCost": 165.00,
                  "budgetBreakdown": {"flights":120.00,"hotels":45.00,"food":0.00,"transport":0.00,"activities":0.00},
                  "tips": ["tip"]
                }
                """;
        when(aiClient.optimizeTrip(any())).thenReturn(json);
        when(tripService.getTrip(10L, "owner@test.com"))
                .thenReturn(new TripResponse(10L, "Barcelona", "Warsaw",
                        trip.getStartDate(), trip.getEndDate(), trip.getBudget(),
                        "EUR", TripStatus.OPTIMIZED, null));

        TripResponse res = service.optimizeTrip(10L, "owner@test.com");

        assertThat(res.status()).isEqualTo(TripStatus.OPTIMIZED);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.OPTIMIZED);
        assertThat(trip.getDays()).hasSize(1);
        assertThat(trip.getDays().get(0).getExpenses()).hasSize(2);
        assertThat(trip.getDays().get(0).getExpenses().get(0).getCategory())
                .isEqualTo(ExpenseCategory.FLIGHT);
        verify(tripRepository).save(trip);
    }

    @Test
    void optimize_markdownFencedJson_isStrippedAndParsed() {
        Trip trip = sampleTrip();
        when(tripService.getOwnedTripEntity(10L, "owner@test.com")).thenReturn(trip);
        String fenced = "```json\n{\"days\":[],\"totalEstimatedCost\":0,\"budgetBreakdown\":null,\"tips\":[]}\n```";
        when(aiClient.optimizeTrip(any())).thenReturn(fenced);
        when(tripService.getTrip(10L, "owner@test.com"))
                .thenReturn(new TripResponse(10L, "Barcelona", "Warsaw",
                        trip.getStartDate(), trip.getEndDate(), trip.getBudget(),
                        "EUR", TripStatus.OPTIMIZED, null));

        TripResponse res = service.optimizeTrip(10L, "owner@test.com");

        assertThat(res.status()).isEqualTo(TripStatus.OPTIMIZED);
        assertThat(trip.getDays()).isEmpty();
    }

    @Test
    void optimize_invalidJson_throwsAiIntegrationException() {
        Trip trip = sampleTrip();
        when(tripService.getOwnedTripEntity(10L, "owner@test.com")).thenReturn(trip);
        when(aiClient.optimizeTrip(any())).thenReturn("это не json");

        assertThrows(AiIntegrationException.class,
                () -> service.optimizeTrip(10L, "owner@test.com"));
    }
}
