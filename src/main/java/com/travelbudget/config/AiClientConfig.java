package com.travelbudget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.client.AiClient;
import com.travelbudget.client.AnthropicAiClient;
import com.travelbudget.client.GeminiAiClient;
import com.travelbudget.client.GroqAiClient;
import com.travelbudget.client.MockAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Выбирает реализацию AiClient по провайдеру/ключу.
 *
 * ai.provider:
 *   gemini    -> GeminiAiClient   (бесплатно)
 *   anthropic -> AnthropicAiClient (платно, нужны кредиты)
 *   auto      -> есть ключ Gemini -> Gemini; иначе есть ключ Anthropic -> Claude; иначе mock
 *   (пусто/mock) -> демо-заглушка
 */
@Configuration
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    @Bean
    public AiClient aiClient(
            @Value("${ai.provider:auto}") String provider,
            @Value("${ai.api.key:}") String anthropicKey,
            @Value("${ai.api.model}") String anthropicModel,
            @Value("${ai.gemini.key:}") String geminiKey,
            @Value("${ai.gemini.model}") String geminiModel,
            @Value("${ai.groq.key:}") String groqKey,
            @Value("${ai.groq.model}") String groqModel,
            ObjectMapper objectMapper) {

        String p = provider == null ? "auto" : provider.trim().toLowerCase();
        boolean auto = p.equals("auto");

        if (p.equals("groq") || (auto && StringUtils.hasText(groqKey))) {
            log.info("AI: используется Groq (модель {})", groqModel);
            return new GroqAiClient(groqKey, groqModel, objectMapper);
        }
        if (p.equals("gemini") || (auto && StringUtils.hasText(geminiKey))) {
            log.info("AI: используется Google Gemini (модель {})", geminiModel);
            return new GeminiAiClient(geminiKey, geminiModel, objectMapper);
        }
        if (p.equals("anthropic") || (auto && StringUtils.hasText(anthropicKey))) {
            log.info("AI: используется Claude (модель {})", anthropicModel);
            return new AnthropicAiClient(anthropicKey, anthropicModel);
        }
        log.info("AI: ключ не задан — используется MockAiClient (демо-план)");
        return new MockAiClient(objectMapper);
    }
}
