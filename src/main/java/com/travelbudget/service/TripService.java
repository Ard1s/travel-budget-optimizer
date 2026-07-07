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
import com.travelbudget.exception.UserNotFoundException;
import com.travelbudget.repository.TripRepository;
import com.travelbudget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Бизнес-логика путешествий.
 *
 * @Transactional(readOnly = true) на классе:
 *   - все методы по умолчанию идут в read-only транзакции (можно безопасно
 *     обращаться к ленивым связям trip.getUser()/getDays());
 *   - методы, которые пишут в БД, помечены @Transactional отдельно (readOnly = false).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripResponse createTrip(CreateTripRequest req, String userEmail) {
        User user = getUserByEmail(userEmail);

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(req.destination());
        trip.setOriginCity(req.originCity());
        trip.setStartDate(req.startDate());
        trip.setEndDate(req.endDate());
        trip.setBudget(req.budget());
        trip.setCurrency(req.currency());
        // Предпочтения (Фаза 1). interests из списка -> строка через запятую.
        trip.setHotelStars(req.hotelStars());
        trip.setAccommodationPreference(req.accommodationPreference());
        trip.setFoodStyle(req.foodStyle());
        trip.setInterests(
                (req.interests() == null || req.interests().isEmpty())
                        ? null
                        : String.join(",", req.interests()));
        // status остаётся DRAFT (значение по умолчанию в сущности)

        return toResponse(tripRepository.save(trip));
    }

    public List<TripResponse> getUserTrips(String userEmail) {
        User user = getUserByEmail(userEmail);
        return tripRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TripResponse getTrip(Long id, String userEmail) {
        return toResponse(getOwnedTripEntity(id, userEmail));
    }

    @Transactional
    public TripResponse updateTrip(Long id, UpdateTripRequest req, String userEmail) {
        Trip trip = getOwnedTripEntity(id, userEmail);

        // Частичное обновление: применяем только переданные (не-null) поля.
        if (req.destination() != null) {
            trip.setDestination(req.destination());
        }
        if (req.originCity() != null) {
            trip.setOriginCity(req.originCity());
        }
        if (req.startDate() != null) {
            trip.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            trip.setEndDate(req.endDate());
        }
        if (req.budget() != null) {
            trip.setBudget(req.budget());
        }
        if (req.currency() != null) {
            trip.setCurrency(req.currency());
        }

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void deleteTrip(Long id, String userEmail) {
        Trip trip = getOwnedTripEntity(id, userEmail);
        tripRepository.delete(trip);
    }

    /**
     * Разбивка бюджета: суммируем оценочные расходы по категориям во всех днях поездки.
     * Обход trip.getDays() и day.getExpenses() безопасен внутри @Transactional (ленивые связи).
     */
    public BudgetBreakdownResponse getBudgetBreakdown(Long id, String userEmail) {
        Trip trip = getOwnedTripEntity(id, userEmail);

        Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);
        BigDecimal total = BigDecimal.ZERO;

        for (TripDay day : trip.getDays()) {
            for (Expense exp : day.getExpenses()) {
                BigDecimal cost = exp.getEstimatedCost() != null ? exp.getEstimatedCost() : BigDecimal.ZERO;
                // merge: если категории ещё нет — положить cost, иначе прибавить.
                byCategory.merge(exp.getCategory(), cost, BigDecimal::add);
                total = total.add(cost);
            }
        }

        BigDecimal remaining = trip.getBudget().subtract(total);
        return new BudgetBreakdownResponse(trip.getBudget(), total, remaining, byCategory);
    }

    // ---------- переиспользуемые помощники ----------

    /**
     * Находит поездку и проверяет, что она принадлежит текущему пользователю.
     * Нет поездки -> 404, чужая -> 403. Вся защита владельца сосредоточена здесь.
     *
     * public — потому что этим же методом пользуется TripOptimizationService,
     * чтобы НЕ дублировать security-логику (её нельзя копировать по сервисам).
     */
    public Trip getOwnedTripEntity(Long id, String userEmail) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new TripNotFoundException(id));

        // Обращение к ленивому user.getEmail() безопасно: мы внутри @Transactional.
        if (!trip.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Это не ваша поездка");
        }
        return trip;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    private TripResponse toResponse(Trip t) {
        return new TripResponse(
                t.getId(),
                t.getDestination(),
                t.getOriginCity(),
                t.getStartDate(),
                t.getEndDate(),
                t.getBudget(),
                t.getCurrency(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}
