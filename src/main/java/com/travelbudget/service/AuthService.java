package com.travelbudget.service;

import com.travelbudget.dto.request.LoginRequest;
import com.travelbudget.dto.request.RegisterRequest;
import com.travelbudget.dto.response.AuthResponse;
import com.travelbudget.entity.Role;
import com.travelbudget.entity.User;
import com.travelbudget.exception.EmailAlreadyExistsException;
import com.travelbudget.exception.UserNotFoundException;
import com.travelbudget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Бизнес-логика регистрации и логина.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация: проверяем уникальность email, хэшируем пароль, сохраняем, выдаём токен.
     * @Transactional — вся операция в одной транзакции (или всё, или ничего).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password())); // BCrypt, не plain text
        user.setName(request.name());
        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }

    /**
     * Логин: AuthenticationManager сам проверит email+пароль через наш UserDetailsService.
     * Если неверно — бросит BadCredentialsException (handler вернёт 401).
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName());
    }
}
