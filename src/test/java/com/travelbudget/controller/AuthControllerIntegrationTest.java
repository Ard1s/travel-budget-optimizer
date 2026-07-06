package com.travelbudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbudget.dto.request.LoginRequest;
import com.travelbudget.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Полный HTTP-путь регистрации/логина через реальный контекст (H2).
 * @Transactional откатывает изменения БД после каждого теста — тесты изолированы.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registerThenLogin_fullFlow() throws Exception {
        RegisterRequest register = new RegisterRequest("int@test.com", "password123", "Int User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("int@test.com"));

        LoginRequest login = new LoginRequest("int@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest register = new RegisterRequest("dupe@test.com", "password123", "Dup");
        String body = objectMapper.writeValueAsString(register);

        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        // короткий пароль + невалидный email + пустое имя
        RegisterRequest bad = new RegisterRequest("not-an-email", "123", "");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
