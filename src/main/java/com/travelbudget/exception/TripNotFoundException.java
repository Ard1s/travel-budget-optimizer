package com.travelbudget.exception;

/**
 * Поездка с таким id не найдена. Обрабатывается -> HTTP 404.
 */
public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(Long id) {
        super("Путешествие с id " + id + " не найдено");
    }
}
