package com.travelbudget.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Центральная конфигурация Spring Security.
 *
 * Ключевые идеи:
 *  - STATELESS: сессий нет, состояние клиента держит JWT. Поэтому же выключаем CSRF
 *    (CSRF-атаки актуальны для cookie-сессий, а у нас токен в заголовке).
 *  - Наш JwtAuthFilter встаёт ПЕРЕД стандартным UsernamePasswordAuthenticationFilter.
 *  - DaoAuthenticationProvider отдельно НЕ объявляем: Spring соберёт его сам
 *    из бинов CustomUserDetailsService + PasswordEncoder.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Неаутентифицированный доступ к защищённому ресурсу -> наш 401 (а не дефолтный 403).
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты:
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Всё остальное требует валидный JWT:
                        .anyRequest().authenticated()
                )
                // H2-консоль рендерится во фрейме; разрешаем фреймы того же origin (только dev).
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt: медленное одностороннее хэширование с солью — стандарт для паролей. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager нужен AuthService для логина.
     * Берём готовый из AuthenticationConfiguration — он уже знает про
     * наш UserDetailsService и PasswordEncoder.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
