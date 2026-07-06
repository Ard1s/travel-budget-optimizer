package com.travelbudget.config;

import com.travelbudget.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр выполняется ОДИН раз на каждый HTTP-запрос (OncePerRequestFilter).
 * Задача: если в заголовке есть валидный "Authorization: Bearer <token>",
 * положить пользователя в SecurityContext, чтобы дальше он считался аутентифицированным.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Нет заголовка или не Bearer — пропускаем дальше без аутентификации.
        // (Для публичных эндпоинтов это ок; защищённые потом вернут 401.)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // убираем "Bearer "

        try {
            String email = jwtService.extractEmail(token);

            // Аутентифицируем, только если ещё не аутентифицированы в этом запросе.
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Битый/просроченный токен: НЕ роняем запрос, просто не аутентифицируем.
            // Защищённый эндпоинт вернёт 401, публичный — отработает.
            logger.debug("Невалидный JWT: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
