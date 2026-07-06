-- Таблица пользователей.
-- BIGSERIAL = bigint + автоинкремент (соответствует @GeneratedValue(IDENTITY) в Postgres).
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
