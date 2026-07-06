-- Таблица путешествий. user_id — внешний ключ на users.
CREATE TABLE trips (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id),
    destination VARCHAR(255)  NOT NULL,
    origin_city VARCHAR(255)  NOT NULL,
    start_date  DATE,
    end_date    DATE,
    budget      DECIMAL(10,2) NOT NULL,
    currency    VARCHAR(10)   NOT NULL,
    status      VARCHAR(50)   NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Индекс на user_id: почти все запросы идут "поездки конкретного пользователя".
CREATE INDEX idx_trips_user_id ON trips (user_id);
