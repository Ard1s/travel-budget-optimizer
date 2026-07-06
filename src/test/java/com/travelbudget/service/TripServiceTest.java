package com.travelbudget.service;

import com.travelbudget.dto.request.CreateTripRequest;
import com.travelbudget.dto.request.UpdateTripRequest;
import com.travelbudget.dto.response.BudgetBreakdownResponse;
import com.travelbudget.dto.response.TripResponse;
import com.travelbudget.entity.Expense;
import com.travelbudget.entity.ExpenseCategory;
import com.travelbudget.entity.Trip;
import com.travelbudget.entity.TripDay;
import com.travelbudget.entity.User;
import com.travelbudget.exception.TripNotFoundException;
import com.travelbudget.repository.TripRepository;
import com.travelbudget.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TripService tripService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@test.com");
    }

    private Trip sampleTrip() {
        Trip trip = new Trip();
        trip.setId(10L);
        trip.setUser(owner);
        trip.setDestination("Barcelona");
        trip.setOriginCity("Warsaw");
        trip.setStartDate(LocalDate.of(2025, 8, 1));
        trip.setEndDate(LocalDate.of(2025, 8, 10));
        trip.setBudget(new BigDecimal("1000.00"));
        trip.setCurrency("EUR");
        return trip;
    }

    @Test
    void createTrip_success() {
        CreateTripRequest req = new CreateTripRequest(
                "Barcelona", "Warsaw",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10),
                new BigDecimal("1000.00"), "EUR");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripResponse res = tripService.createTrip(req, "owner@test.com");

        assertThat(res.destination()).isEqualTo("Barcelona");
        assertThat(res.budget()).isEqualByComparingTo("1000.00");
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void getUserTrips_returnsList() {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(tripRepository.findByUserOrderByCreatedAtDesc(owner))
                .thenReturn(List.of(sampleTrip()));

        List<TripResponse> res = tripService.getUserTrips("owner@test.com");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).destination()).isEqualTo("Barcelona");
    }

    @Test
    void getTrip_owner_ok() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(sampleTrip()));

        TripResponse res = tripService.getTrip(10L, "owner@test.com");

        assertThat(res.id()).isEqualTo(10L);
    }

    @Test
    void getTrip_wrongUser_throwsAccessDenied() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(sampleTrip()));

        assertThrows(AccessDeniedException.class,
                () -> tripService.getTrip(10L, "intruder@test.com"));
    }

    @Test
    void getTrip_notFound_throws() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TripNotFoundException.class,
                () -> tripService.getTrip(99L, "owner@test.com"));
    }

    @Test
    void updateTrip_partial_onlyChangesProvidedFields() {
        Trip trip = sampleTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        // Меняем только бюджет; остальное null.
        UpdateTripRequest req = new UpdateTripRequest(
                null, null, null, null, new BigDecimal("1500.00"), null);

        TripResponse res = tripService.updateTrip(10L, req, "owner@test.com");

        assertThat(res.budget()).isEqualByComparingTo("1500.00");
        assertThat(res.destination()).isEqualTo("Barcelona"); // не затёрлось
    }

    @Test
    void deleteTrip_owner_deletes() {
        Trip trip = sampleTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripService.deleteTrip(10L, "owner@test.com");

        verify(tripRepository).delete(trip);
    }

    @Test
    void getBudgetBreakdown_sumsByCategory() {
        Trip trip = sampleTrip();
        TripDay day = new TripDay();
        day.setTrip(trip);
        day.getExpenses().add(expense(ExpenseCategory.FLIGHT, "120.00"));
        day.getExpenses().add(expense(ExpenseCategory.HOTEL, "45.00"));
        day.getExpenses().add(expense(ExpenseCategory.HOTEL, "55.00"));
        trip.getDays().add(day);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        BudgetBreakdownResponse res = tripService.getBudgetBreakdown(10L, "owner@test.com");

        assertThat(res.totalEstimated()).isEqualByComparingTo("220.00");
        assertThat(res.remaining()).isEqualByComparingTo("780.00"); // 1000 - 220
        assertThat(res.byCategory().get(ExpenseCategory.HOTEL)).isEqualByComparingTo("100.00");
        assertThat(res.byCategory().get(ExpenseCategory.FLIGHT)).isEqualByComparingTo("120.00");
    }

    private Expense expense(ExpenseCategory category, String cost) {
        Expense e = new Expense();
        e.setCategory(category);
        e.setEstimatedCost(new BigDecimal(cost));
        return e;
    }
}
