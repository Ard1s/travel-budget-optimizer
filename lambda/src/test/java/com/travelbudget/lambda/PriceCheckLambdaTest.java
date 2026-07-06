package com.travelbudget.lambda;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тестируем чистую логику решения об уведомлении.
 * Интеграцию с AWS/HTTP здесь не трогаем — это уже интеграционный уровень.
 */
class PriceCheckLambdaTest {

    @Test
    void notify_whenCurrentBelowTarget() {
        assertTrue(PriceCheckLambda.shouldNotify(new BigDecimal("90.00"), new BigDecimal("100.00")));
    }

    @Test
    void notify_whenCurrentEqualsTarget() {
        assertTrue(PriceCheckLambda.shouldNotify(new BigDecimal("100.00"), new BigDecimal("100.00")));
    }

    @Test
    void doNotNotify_whenCurrentAboveTarget() {
        assertFalse(PriceCheckLambda.shouldNotify(new BigDecimal("120.00"), new BigDecimal("100.00")));
    }

    @Test
    void doNotNotify_whenPriceUnknown() {
        assertFalse(PriceCheckLambda.shouldNotify(null, new BigDecimal("100.00")));
    }
}
