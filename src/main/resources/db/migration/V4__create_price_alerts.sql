-- Подписки на снижение цены. Читаются AWS Lambda (Модуль 7).
CREATE TABLE price_alerts (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id),
    origin       VARCHAR(255)  NOT NULL,
    destination  VARCHAR(255)  NOT NULL,
    target_date  DATE,
    target_price DECIMAL(10,2),
    currency     VARCHAR(10),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    last_checked TIMESTAMP
);

CREATE INDEX idx_price_alerts_user_id ON price_alerts (user_id);
-- Частичный индекс: Lambda берёт только активные подписки.
CREATE INDEX idx_price_alerts_active ON price_alerts (active) WHERE active = TRUE;
