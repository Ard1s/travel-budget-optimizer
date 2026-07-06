package com.travelbudget.service;

import com.travelbudget.entity.Role;
import com.travelbudget.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-тест JwtService. Контекст Spring не поднимаем — создаём объект руками
 * и через ReflectionTestUtils подставляем поля, которые в бою приходят из @Value.
 */
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "test-secret-key-minimum-256-bits-long-for-hs256-abcdef0123456789";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L); // 1 час
    }

    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setRole(Role.USER);
        return u;
    }

    private UserDetails userDetails(String email) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email).password("x").authorities("ROLE_USER").build();
    }

    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        String token = jwtService.generateToken(user("nika@test.com"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("nika@test.com");
    }

    @Test
    void isTokenValid_true_forMatchingUser() {
        String token = jwtService.generateToken(user("nika@test.com"));

        assertThat(jwtService.isTokenValid(token, userDetails("nika@test.com"))).isTrue();
    }

    @Test
    void isTokenValid_false_whenEmailMismatch() {
        String token = jwtService.generateToken(user("nika@test.com"));

        assertThat(jwtService.isTokenValid(token, userDetails("other@test.com"))).isFalse();
    }

    @Test
    void expiredToken_throwsOnParse() {
        // Отрицательный срок => токен уже истёк в момент создания.
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L);
        String expired = jwtService.generateToken(user("nika@test.com"));

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractEmail(expired));
    }

    @Test
    void tamperedToken_throws() {
        String token = jwtService.generateToken(user("nika@test.com"));
        String tampered = token.substring(0, token.length() - 3) + "abc";

        // Подпись/формат не сойдутся -> какое-то JwtException.
        assertThrows(JwtException.class, () -> jwtService.extractEmail(tampered));
    }
}
