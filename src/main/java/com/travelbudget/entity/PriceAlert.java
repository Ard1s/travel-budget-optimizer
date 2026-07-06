package com.travelbudget.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Подписка пользователя на снижение цены авиабилета.
 * Эти записи читает Lambda (Модуль 7) и шлёт уведомление, когда цена упала.
 */
@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(length = 10)
    private String currency;

    /*
     * Примитив boolean (не Boolean) => в БД колонка NOT NULL.
     * Значение по умолчанию true задаём и в Java, и в миграции.
     */
    @Column(nullable = false)
    private boolean active = true;

    // Когда Lambda последний раз проверяла цену.
    @Column(name = "last_checked")
    private LocalDateTime lastChecked;
}
