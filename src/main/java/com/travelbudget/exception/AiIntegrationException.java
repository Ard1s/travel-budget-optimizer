package com.travelbudget.exception;

/**
 * Ошибка интеграции с AI (нет ключа, API вернул ошибку, не разобрали ответ).
 * Обрабатывается -> HTTP 502 Bad Gateway: проблема во внешнем сервисе, не в запросе клиента.
 */
public class AiIntegrationException extends RuntimeException {

    public AiIntegrationException(String message) {
        super(message);
    }
}
