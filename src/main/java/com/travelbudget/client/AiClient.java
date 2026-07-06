package com.travelbudget.client;

/**
 * Абстракция вызова AI. Две реализации:
 *  - AnthropicAiClient — реальный Claude API (профиль по умолчанию);
 *  - MockAiClient      — заглушка для локальной разработки/тестов (профиль "local").
 *
 * TripOptimizationService зависит от этого интерфейса, а не от конкретной реализации —
 * это принцип "инверсии зависимостей": логику можно тестировать без сети и ключа.
 *
 * Метод возвращает СЫРУЮ строку-ответ (JSON-план в тексте), парсит её вызывающая сторона.
 */
public interface AiClient {

    String optimizeTrip(TripOptimizationRequest request);
}
