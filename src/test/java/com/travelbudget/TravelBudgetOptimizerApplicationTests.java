package com.travelbudget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke-тест: поднимается ли контекст Spring целиком.
 * @ActiveProfiles("test") — используем H2 из application-test.yml, а не Postgres,
 * иначе тест требовал бы живую БД.
 */
@SpringBootTest
@ActiveProfiles("test")
class TravelBudgetOptimizerApplicationTests {

    @Test
    void contextLoads() {
        // Если контекст не поднимется — тест упадёт сам.
    }
}
