package com.travelbudget.client;

import com.travelbudget.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Структура JSON-ответа AI (то, что мы просим модель вернуть).
 * Вложенные записи держим здесь же — это единая "форма" ответа.
 *
 * Jackson распарсит:
 *  - "2025-08-01" -> LocalDate (модуль JavaTime подключён Spring Boot автоматически);
 *  - "FLIGHT" -> ExpenseCategory (enum по имени);
 *  - неизвестные лишние поля игнорируются (в Spring Boot это дефолт).
 */
public record AiTripPlan(
        List<Day> days,
        BigDecimal totalEstimatedCost,
        Breakdown budgetBreakdown,
        List<String> tips
) {

    public record Day(
            LocalDate date,
            String city,
            String description,
            List<Expense> expenses
    ) {
    }

    public record Expense(
            ExpenseCategory category,
            String description,
            BigDecimal cost
    ) {
    }

    public record Breakdown(
            BigDecimal flights,
            BigDecimal hotels,
            BigDecimal food,
            BigDecimal transport,
            BigDecimal activities
    ) {
    }
}
