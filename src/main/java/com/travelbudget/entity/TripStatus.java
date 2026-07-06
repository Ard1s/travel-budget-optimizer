package com.travelbudget.entity;

/**
 * Статус путешествия:
 *  DRAFT     — создано, но ещё не оптимизировано AI;
 *  OPTIMIZED — AI построил маршрут (заполнены дни и расходы);
 *  CONFIRMED — пользователь подтвердил план.
 */
public enum TripStatus {
    DRAFT,
    OPTIMIZED,
    CONFIRMED
}
