package com.travelbudget.service;

import com.travelbudget.entity.User;
import com.travelbudget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Мост между нашей сущностью User и моделью Spring Security (UserDetails).
 *
 * Spring Security при логине и при проверке токена вызывает loadUserByUsername(...).
 * У нас "username" = email.
 *
 * @RequiredArgsConstructor (Lombok) генерирует конструктор для final-полей —
 * так работает инъекция зависимостей без @Autowired.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + email));

        // Роль превращаем в GrantedAuthority с префиксом ROLE_ —
        // это соглашение Spring Security (нужно для hasRole('ADMIN') в будущем).
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // BCrypt-хэш
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();
    }
}
