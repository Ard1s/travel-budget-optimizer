package com.travelbudget.entity;

/**
 * Пожелание по расположению жилья. Хранится строкой в БД.
 *  NEAR_SEA    — у моря/пляжа (обычно дороже);
 *  CITY_CENTER — в центре;
 *  QUIET       — тихий район;
 *  ANY         — не важно.
 */
public enum AccommodationPreference {
    NEAR_SEA,
    CITY_CENTER,
    QUIET,
    ANY
}
