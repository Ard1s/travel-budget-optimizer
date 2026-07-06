package com.travelbudget.dto.response;

import com.travelbudget.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Разбивка бюджета поездки:
 *  - budget        — заявленный бюджет;
 *  - totalEstimated — сумма всех оценочных расходов;
 *  - remaining     — сколько осталось (budget - totalEstimated), может быть отрицательным;
 *  - byCategory    — сумма по каждой категории (Jackson отдаст объект с ключами-строками enum).
 */
public record BudgetBreakdownResponse(
        BigDecimal budget,
        BigDecimal totalEstimated,
        BigDecimal remaining,
        Map<ExpenseCategory, BigDecimal> byCategory
) {
}
