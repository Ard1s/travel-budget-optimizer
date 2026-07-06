package com.travelbudget.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

/**
 * Lambda мониторинга цен. Триггер — EventBridge по расписанию (напр. каждый час).
 *
 * Поток:
 *   1. забрать активные подписки из нашего API;
 *   2. по каждой узнать текущую цену билета;
 *   3. если цена <= целевой — опубликовать сообщение в SNS (оно уйдёт подписчикам на email).
 *
 * ENV-переменные (задаются в конфигурации Lambda): API_BASE_URL, API_KEY, SNS_TOPIC_ARN.
 */
public class PriceCheckLambda implements RequestHandler<ScheduledEvent, String> {

    private final SnsClient snsClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String snsTopicArn;

    /**
     * Конструктор по умолчанию — его вызывает среда Lambda.
     * Создаёт "тяжёлые" клиенты один раз (переиспользуются между вызовами при тёплом старте).
     */
    public PriceCheckLambda() {
        this(
                SnsClient.create(),
                HttpClient.newHttpClient(),
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                System.getenv("API_BASE_URL"),
                System.getenv("API_KEY"),
                System.getenv("SNS_TOPIC_ARN")
        );
    }

    /**
     * Конструктор для тестов — позволяет подсунуть моки клиентов и значения ENV.
     * (package-private, чтобы тест из того же пакета мог его вызвать)
     */
    PriceCheckLambda(SnsClient snsClient, HttpClient httpClient, ObjectMapper objectMapper,
                     String apiBaseUrl, String apiKey, String snsTopicArn) {
        this.snsClient = snsClient;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
        this.snsTopicArn = snsTopicArn;
    }

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Price check started at " + Instant.now());

        try {
            List<PriceAlertDto> alerts = fetchActiveAlerts();
            logger.log("Active alerts found: " + alerts.size());

            int notified = 0;
            for (PriceAlertDto alert : alerts) {
                BigDecimal currentPrice = fetchCurrentPrice(alert);
                if (shouldNotify(currentPrice, alert.targetPrice())) {
                    sendNotification(alert, currentPrice);
                    notified++;
                    logger.log("Notification sent: " + alert.origin() + " -> " + alert.destination());
                }
            }
            return "OK checked=" + alerts.size() + " notified=" + notified;

        } catch (Exception e) {
            // Логируем и пробрасываем: ненулевой результат => CloudWatch пометит запуск как ошибочный.
            logger.log("ERROR: " + e.getMessage());
            throw new RuntimeException("Price check failed", e);
        }
    }

    /**
     * Решение об уведомлении. Вынесено в static-метод — чистая логика, легко тестируется.
     * Уведомляем, если текущая цена известна и не выше целевой.
     */
    static boolean shouldNotify(BigDecimal currentPrice, BigDecimal targetPrice) {
        return currentPrice != null
                && targetPrice != null
                && currentPrice.compareTo(targetPrice) <= 0;
    }

    private List<PriceAlertDto> fetchActiveAlerts() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/api/alerts/active"))
                .header("X-Api-Key", apiKey == null ? "" : apiKey) // сервисная авторизация Lambda -> API
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("API вернул статус " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<List<PriceAlertDto>>() {
        });
    }

    /**
     * ЗАГЛУШКА. В реальном проекте здесь вызов внешнего API цен на авиабилеты
     * (Amadeus / Skyscanner / Kiwi и т.п.) по маршруту alert.origin -> alert.destination.
     * Для демонстрации возвращаем целевую цену, чтобы показать срабатывание уведомления.
     */
    private BigDecimal fetchCurrentPrice(PriceAlertDto alert) {
        // TODO: интеграция с реальным провайдером цен.
        return alert.targetPrice();
    }

    private void sendNotification(PriceAlertDto alert, BigDecimal price) {
        String message = String.format(
                "Цена на билет %s -> %s упала до %s %s! Подробнее: %s/alerts/%d",
                alert.origin(), alert.destination(), price, alert.currency(), apiBaseUrl, alert.id()
        );

        snsClient.publish(PublishRequest.builder()
                .topicArn(snsTopicArn)
                .subject("Price drop alert") // тема SNS — только ASCII
                .message(message)
                .build());
    }
}
