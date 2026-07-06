package com.travelbudget.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса POST /api/auth/register.
 *
 * record — неизменяемый DTO (Java 16+): компилятор сам сделает конструктор,
 * геттеры (email(), password(), name()), equals/hashCode/toString.
 * Идеально для входящих/исходящих данных.
 *
 * Аннотации валидации проверяются, когда в контроллере стоит @Valid.
 * Если не проходят — Spring бросает MethodArgumentNotValidException (наш handler вернёт 400).
 */
public record RegisterRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, message = "Пароль должен быть не короче 8 символов")
        String password,

        @NotBlank(message = "Имя обязательно")
        String name
) {
}
