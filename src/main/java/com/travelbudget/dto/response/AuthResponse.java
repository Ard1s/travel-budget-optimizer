package com.travelbudget.dto.response;

/**
 * Ответ на регистрацию/логин: JWT-токен + базовые данные пользователя.
 * Пароль/хэш сюда НИКОГДА не кладём.
 */
public record AuthResponse(
        String token,
        String email,
        String name
) {
}
