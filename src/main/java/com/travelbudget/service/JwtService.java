package com.travelbudget.service;

import com.travelbudget.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Генерация и проверка JWT (библиотека jjwt 0.12.x).
 *
 * Токен — это подписанная строка вида header.payload.signature.
 * Подпись (HS256) делается секретным ключом: изменить payload, не зная секрета, нельзя.
 * Сервер не хранит токены — проверяет подпись математически (stateless).
 */
@Service
public class JwtService {

    // Значения приходят из application.yml (app.jwt.secret / app.jwt.expiration),
    // которые в свою очередь берут из ENV. В коде секрета нет (правило проекта №3).
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration; // мс

    /**
     * Создаём токен: subject = email, отдельный claim role, время выпуска и истечения.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name()) // кладём строку "USER"/"ADMIN"
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** Достаём email (subject) из токена. Бросит JwtException, если подпись/срок неверны. */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Токен валиден, если email в нём совпал с пользователем И срок не истёк. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /** Парсим и одновременно проверяем подпись ключом (verifyWith). */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Строим ключ из секрета. Для HS256 нужно >= 256 бит (>= 32 байта),
     * иначе Keys.hmacShaKeyFor бросит исключение. Поэтому секрет в yml длинный.
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
