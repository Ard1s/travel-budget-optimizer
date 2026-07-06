package com.travelbudget.service;

import com.travelbudget.dto.request.LoginRequest;
import com.travelbudget.dto.request.RegisterRequest;
import com.travelbudget.dto.response.AuthResponse;
import com.travelbudget.entity.Role;
import com.travelbudget.entity.User;
import com.travelbudget.exception.EmailAlreadyExistsException;
import com.travelbudget.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @Test
    void register_success_savesUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest("nika@test.com", "password123", "Nika");
        when(userRepository.existsByEmail("nika@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse res = authService.register(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.email()).isEqualTo("nika@test.com");
        assertThat(res.name()).isEqualTo("Nika");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        RegisterRequest req = new RegisterRequest("dupe@test.com", "password123", "Dup");
        when(userRepository.existsByEmail("dupe@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest req = new LoginRequest("nika@test.com", "password123");
        User user = new User();
        user.setEmail("nika@test.com");
        user.setName("Nika");
        user.setRole(Role.USER);
        when(userRepository.findByEmail("nika@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse res = authService.login(req);

        assertThat(res.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_badCredentials_propagatesAndSkipsLookup() {
        LoginRequest req = new LoginRequest("nika@test.com", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(userRepository, never()).findByEmail(any());
    }
}
