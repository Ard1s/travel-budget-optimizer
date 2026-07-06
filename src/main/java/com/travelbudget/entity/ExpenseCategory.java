package com.travelbudget.entity;

/**
 * Категория расхода в путешествии. Хранится строкой в БД.
 * Используется и в разбивке бюджета (budget-breakdown).
 */
public enum ExpenseCategory {
    FLIGHT,
    HOTEL,
    FOOD,
    TRANSPORT,
    ACTIVITIES,
    OTHER
}
