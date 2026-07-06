package com.travelbudget.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Отдельный расход внутри дня путешествия (билет, отель, еда и т.д.).
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExpenseCategory category;   // FLIGHT, HOTEL, FOOD, TRANSPORT, ACTIVITIES, OTHER

    private String description;

    // Оценка стоимости от AI.
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    // Фактическая стоимость — null, пока пользователь не подтвердил.
    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;
}
