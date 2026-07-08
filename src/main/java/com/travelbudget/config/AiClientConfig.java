package com.travelbudget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.client.AiClient;
import com.travelbudget.client.AnthropicAiClient;
import com.travelbudget.client.MockAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Выбирает реализацию AiClient ПО НАЛИЧИЮ КЛЮЧА, а не по профилю.
 * Так реальный Claude работает в любом профиле (в т.ч. local на H2), если задан ai.api.key;
 * без ключа — демо-мок. Это удобно: ключ включает "настоящий" режим одной строкой.
 */
@Configuration
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    @Bean
    public AiClient aiClient(
            @Value("${ai.api.key:}") String apiKey,
            @Value("${ai.api.model}") String model,
            ObjectMapper objectMapper) {

        if (StringUtils.hasText(apiKey)) {
            log.info("AI: используется реальный Claude (модель {})", model);
            return new AnthropicAiClient(apiKey, model);
        }
        log.info("AI: ключ не задан — используется MockAiClient (демо-план)");
        return new MockAiClient(objectMapper);
    }
}
