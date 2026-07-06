package com.travelbudget.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Пользователь системы.
 *
 * Про Lombok: используем @Getter/@Setter/@NoArgsConstructor.
 * НЕ используем @Data, потому что он добавляет @ToString/@EqualsAndHashCode
 * по ВСЕМ полям — для сущностей это источник багов (обращение к ленивым связям
 * вне транзакции, рекурсия). Для User связей нет, но держим единый стиль.
 *
 * @EntityListeners(AuditingEntityListener.class) — включает автозаполнение
 * поля @CreatedDate при вставке (работает вместе с @EnableJpaAuditing в конфиге).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    // IDENTITY = БД сама генерирует id (в Postgres это BIGSERIAL).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Тут лежит BCrypt-хэш, никогда не plain text (правило проекта №2).
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    // EnumType.STRING → в БД пишется "USER"/"ADMIN".
    // ВАЖНО не использовать ORDINAL: при добавлении новой роли в середину enum
    // числовые значения "поедут" и данные испортятся.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.USER;

    // updatable = false — дата создания не меняется после вставки.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
