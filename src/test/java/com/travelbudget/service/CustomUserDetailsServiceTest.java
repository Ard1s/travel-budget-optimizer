package com.travelbudget.service;

import com.travelbudget.entity.Role;
import com.travelbudget.entity.User;
import com.travelbudget.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_found_mapsToUserDetails() {
        User user = new User();
        user.setEmail("nika@test.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        when(userRepository.findByEmail("nika@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("nika@test.com");

        assertThat(details.getUsername()).isEqualTo("nika@test.com");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost@test.com"));
    }
}
