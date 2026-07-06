package com.travelbudget.repository;

import com.travelbudget.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий пользователей.
 * Spring Data JPA сам генерирует реализацию по именам методов:
 *  - findByEmail  -> SELECT ... WHERE email = ?
 *  - existsByEmail -> SELECT COUNT(...) > 0 WHERE email = ?
 *
 * Optional<User> вместо User — чтобы явно обрабатывать "не найдено", а не ловить null.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
