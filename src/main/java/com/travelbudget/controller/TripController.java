package com.travelbudget.controller;

import com.travelbudget.dto.request.CreateTripRequest;
import com.travelbudget.dto.request.UpdateTripRequest;
import com.travelbudget.dto.response.BudgetBreakdownResponse;
import com.travelbudget.dto.response.TripResponse;
import com.travelbudget.service.TripOptimizationService;
import com.travelbudget.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

/**
 * CRUD путешествий. Все эндпоинты требуют JWT (защищены в SecurityConfig).
 *
 * @AuthenticationPrincipal UserDetails user — это тот принципал, которого положил
 * в SecurityContext наш JwtAuthFilter. user.getUsername() = email пользователя.
 * Передаём email в сервис, чтобы тот проверял владельца.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Управление путешествиями")
public class TripController {

    private final TripService tripService;
    private final TripOptimizationService tripOptimizationService;

    @PostMapping
    @Operation(summary = "Создать новое путешествие")
    public ResponseEntity<TripResponse> create(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        TripResponse created = tripService.createTrip(request, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(created); // 201
    }

    @GetMapping
    @Operation(summary = "Список моих путешествий")
    public ResponseEntity<List<TripResponse>> getAll(
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(tripService.getUserTrips(user.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Одно путешествие по id")
    public ResponseEntity<TripResponse> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(tripService.getTrip(id, user.getUsername()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить путешествие")
    public ResponseEntity<TripResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTripRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(tripService.updateTrip(id, request, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    @Operation(summary = "Удалить путешествие")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        tripService.deleteTrip(id, user.getUsername());
    }

    @PostMapping("/{id}/optimize")
    @Operation(summary = "Запустить AI-оптимизацию маршрута")
    public ResponseEntity<TripResponse> optimize(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(tripOptimizationService.optimizeTrip(id, user.getUsername()));
    }

    @GetMapping("/{id}/budget-breakdown")
    @Operation(summary = "Разбивка бюджета по категориям")
    public ResponseEntity<BudgetBreakdownResponse> budgetBreakdown(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(tripService.getBudgetBreakdown(id, user.getUsername()));
    }
}
