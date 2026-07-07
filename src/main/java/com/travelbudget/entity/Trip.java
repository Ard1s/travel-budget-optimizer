package com.travelbudget.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Путешествие пользователя.
 *
 * Связи:
 *  - many trips -> one user  (@ManyToOne, LAZY: user грузится только при обращении);
 *  - one trip  -> many days  (@OneToMany, cascade ALL + orphanRemoval:
 *    сохраняем/удаляем дни вместе с поездкой).
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * FetchType.LAZY — правильный дефолт для @ManyToOne в реальных проектах:
     * не тянем User из БД, пока он реально не понадобился.
     * @JoinColumn(nullable = false) => колонка user_id NOT NULL с внешним ключом.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String destination;      // "Barcelona, Spain"

    @Column(name = "origin_city", nullable = false)
    private String originCity;       // "Warsaw, Poland"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Деньги ВСЕГДА BigDecimal, не double (иначе ошибки округления).
    // precision/scale = 10 знаков всего, 2 после запятой => DECIMAL(10,2).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, length = 10)
    private String currency;         // "EUR"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TripStatus status = TripStatus.DRAFT;

    // ----- Предпочтения путешественника (Фаза 1). Все необязательные. -----

    // Минимальное число звёзд отеля (1..5).
    @Column(name = "hotel_stars")
    private Integer hotelStars;

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation_preference", length = 30)
    private AccommodationPreference accommodationPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_style", length = 30)
    private FoodStyle foodStyle;

    // Интересы (что посмотреть): коды через запятую, напр. "BEACH,MUSEUMS,NATURE".
    @Column(length = 500)
    private String interests;

    /*
     * mappedBy = "trip" => владелец связи — поле trip в TripDay (там внешний ключ).
     * orphanRemoval = true => если удалить день из этого списка, он удалится и из БД.
     * Инициализируем пустым списком, чтобы не ловить NPE при trip.getDays().add(...).
     */
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDay> days = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
