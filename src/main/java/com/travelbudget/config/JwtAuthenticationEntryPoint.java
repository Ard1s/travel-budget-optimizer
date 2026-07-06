package com.travelbudget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Вызывается Spring Security, когда к защищённому ресурсу обращаются БЕЗ валидной аутентификации.
 * По умолчанию Spring отдаёт пустой 403 — мы же возвращаем осмысленный 401 в JSON-формате,
 * едином со всеми остальными ошибками API.
 *
 * ObjectMapper (для сериализации в JSON) уже есть в контексте — его настраивает Spring Boot.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                "UNAUTHORIZED",
                "Требуется аутентификация: отсутствует или недействителен токен"
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
