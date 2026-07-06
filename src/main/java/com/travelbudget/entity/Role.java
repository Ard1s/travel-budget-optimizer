package com.travelbudget.entity;

/**
 * Роль пользователя. Хранится в БД строкой (@Enumerated(EnumType.STRING)),
 * поэтому важен именно текст "USER"/"ADMIN", а не порядковый номер.
 */
public enum Role {
    USER,
    ADMIN
}
