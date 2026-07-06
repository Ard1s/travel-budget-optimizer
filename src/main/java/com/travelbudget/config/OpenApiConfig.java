package com.travelbudget.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Настройка OpenAPI/Swagger.
 *
 * @SecurityScheme описывает схему "bearerAuth" (JWT в заголовке Authorization).
 * @SecurityRequirement применяет её глобально — в Swagger UI появляется кнопка "Authorize",
 * куда можно вставить токен и дёргать защищённые эндпоинты прямо из браузера.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Travel Budget Optimizer API",
                version = "v1",
                description = "AI-планирование путешествий по бюджету + алёрты по ценам"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
