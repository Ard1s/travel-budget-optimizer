package com.travelbudget.exception;

/**
 * Бросаем при попытке зарегистрировать email, который уже занят.
 * Наследуем RuntimeException (unchecked) — не нужно прописывать throws повсюду.
 * Обрабатывается в GlobalExceptionHandler -> HTTP 409.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Пользователь с email '" + email + "' уже существует");
    }
}
