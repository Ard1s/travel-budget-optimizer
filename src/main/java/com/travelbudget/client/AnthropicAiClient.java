package com.travelbudget.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.travelbudget.exception.AiIntegrationException;

import java.util.stream.Collectors;

/**
 * Реальный клиент Claude через официальный Java SDK Anthropic.
 * Создаётся в AiClientConfig только когда выбран провайдер anthropic и задан ключ.
 * Платный (нужны кредиты на аккаунте Anthropic).
 */
public class AnthropicAiClient implements AiClient {

    private static final long MAX_TOKENS = 8192L;

    private final AnthropicClient client;
    private final String model;

    public AnthropicAiClient(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public String optimizeTrip(TripOptimizationRequest request) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(TripPromptBuilder.build(request))
                .build();

        try {
            Message response = client.messages().create(params);

            String json = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining());

            if (json.isBlank()) {
                throw new AiIntegrationException("Пустой ответ от Claude");
            }
            return json;

        } catch (AiIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiIntegrationException("Ошибка вызова Claude: " + e.getMessage());
        }
    }
}
