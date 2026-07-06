package com.travelbudget.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Тело запроса POST /api/auth/login.
 * Здесь у пароля нет @Size: длину проверяли при регистрации,
 * а при логине важно лишь совпадение с хэшем в БД.
 */
public record LoginRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email,

        @NotBlank(message = "Пароль обязателен")
        String password
) {
}
