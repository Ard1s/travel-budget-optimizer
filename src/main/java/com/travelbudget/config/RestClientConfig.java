package com.travelbudget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Настройка HTTP-клиента для вызова AI.
 *
 * RestClient — современный синхронный HTTP-клиент Spring (замена RestTemplate).
 * Таймауты обязательны: без них зависший внешний API "подвесит" наш поток надолго.
 * AI-ответы бывают долгими, поэтому read-timeout щедрый (120с), connect — короткий.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10с на установку соединения
        factory.setReadTimeout(120_000);   // 120с на ответ (генерация AI)

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
