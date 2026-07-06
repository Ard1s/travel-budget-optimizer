package com.travelbudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.client.AiClient;
import com.travelbudget.dto.request.CreateTripRequest;
import com.travelbudget.dto.request.RegisterRequest;
import com.travelbudget.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Подменяем реальный AnthropicAiClient моком — никакой сети в тестах.
    @MockitoBean private AiClient aiClient;

    /** Регистрирует пользователя и возвращает его JWT. */
    private String registerAndToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "password123", "User");
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    private String createTripBody() throws Exception {
        return objectMapper.writeValueAsString(new CreateTripRequest(
                "Barcelona", "Warsaw",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10),
                new BigDecimal("1000.00"), "EUR"));
    }

    @Test
    void createTripAndList_withToken() throws Exception {
        String token = registerAndToken("trip@test.com");

        mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(createTripBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("Barcelona"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/trips")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destination").value("Barcelona"));
    }

    @Test
    void listTrips_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/trips"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void optimizeAndBudgetBreakdown() throws Exception {
        String token = registerAndToken("opt@test.com");

        // Создаём поездку, забираем её id.
        String createResponse = mockMvc.perform(post("/api/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(createTripBody()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tripId = objectMapper.readTree(createResponse).get("id").asLong();

        // Мок AI возвращает готовый план.
        String plan = """
                {"days":[{"date":"2025-08-01","city":"Barcelona","description":"Day 1",
                  "expenses":[{"category":"FLIGHT","description":"flight","cost":120.00},
                              {"category":"HOTEL","description":"hotel","cost":45.00}]}],
                 "totalEstimatedCost":165.00,
                 "budgetBreakdown":{"flights":120.00,"hotels":45.00,"food":0.00,"transport":0.00,"activities":0.00},
                 "tips":["tip"]}
                """;
        when(aiClient.optimizeTrip(any())).thenReturn(plan);

        mockMvc.perform(post("/api/trips/{id}/optimize", tripId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPTIMIZED"));

        mockMvc.perform(get("/api/trips/{id}/budget-breakdown", tripId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.FLIGHT").exists())
                .andExpect(jsonPath("$.budget").exists());
    }
}
