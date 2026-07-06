package com.travelbudget.exception;

/**
 * Пользователь не найден (по email или id). Обрабатывается -> HTTP 404.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String identifier) {
        super("Пользователь не найден: " + identifier);
    }
}
