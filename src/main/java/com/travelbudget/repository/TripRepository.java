package com.travelbudget.repository;

import com.travelbudget.entity.Trip;
import com.travelbudget.entity.TripStatus;
import com.travelbudget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Репозиторий путешествий.
 *
 * JPQL в @Query проверяется Spring'ом ПРИ СТАРТЕ приложения — если ошибёшься
 * в имени поля, контекст не поднимется. Это бесплатная проверка корректности.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Производный запрос по имени метода: поездки пользователя, новые сверху.
    List<Trip> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT t FROM Trip t WHERE t.user = :user AND t.status = :status")
    List<Trip> findByUserAndStatus(@Param("user") User user,
                                   @Param("status") TripStatus status);

    // Пригодится позже (например, для напоминаний): поездки в диапазоне дат.
    @Query("SELECT t FROM Trip t WHERE t.user.id = :userId "
            + "AND t.startDate BETWEEN :from AND :to")
    List<Trip> findUpcomingTrips(@Param("userId") Long userId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}
