package com.travelbudget.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Один день путешествия. Генерируется AI при оптимизации маршрута.
 */
@Entity
@Table(name = "trip_days")
@Getter
@Setter
@NoArgsConstructor
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /*
     * Поле называется date, но колонку называем trip_date.
     * Почему: DATE — ключевое слово SQL, и "date" как имя колонки в разных СУБД
     * ведёт себя по-разному (где-то требует кавычек). Явный trip_date снимает риск.
     * Это типичный senior-приём: не даём полям имена, совпадающие с зарезервированными словами.
     */
    @Column(name = "trip_date")
    private LocalDate date;

    private String city;

    // Описание генерирует AI — оно длиннее обычной строки, поэтому length = 2000.
    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();
}
