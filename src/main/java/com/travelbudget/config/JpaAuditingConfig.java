package com.travelbudget.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Включает JPA-аудит на уровне приложения.
 * Без @EnableJpaAuditing аннотации @CreatedDate (и @LastModifiedDate) не работают —
 * поля просто останутся null.
 *
 * Держим это в отдельном @Configuration, а не на главном классе приложения:
 * так в тестах проще при необходимости не поднимать аудит.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
