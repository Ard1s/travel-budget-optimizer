package com.travelbudget.controller;

import com.travelbudget.dto.request.LoginRequest;
import com.travelbudget.dto.request.RegisterRequest;
import com.travelbudget.dto.response.AuthResponse;
import com.travelbudget.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Публичные эндпоинты авторизации (открыты в SecurityConfig).
 *
 * @Valid запускает проверку аннотаций из RegisterRequest/LoginRequest.
 * Контроллер тонкий: только принять/провалидировать/отдать. Логика — в AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация и авторизация")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход и получение JWT токена")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
