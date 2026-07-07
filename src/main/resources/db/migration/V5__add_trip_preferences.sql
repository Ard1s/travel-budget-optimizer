-- Фаза 1: предпочтения путешественника. Все колонки необязательные (NULL допустим).
ALTER TABLE trips ADD COLUMN hotel_stars INTEGER;
ALTER TABLE trips ADD COLUMN accommodation_preference VARCHAR(30);
ALTER TABLE trips ADD COLUMN food_style VARCHAR(30);
ALTER TABLE trips ADD COLUMN interests VARCHAR(500);
