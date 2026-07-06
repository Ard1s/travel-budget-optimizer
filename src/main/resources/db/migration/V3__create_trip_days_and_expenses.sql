-- Дни путешествия. trip_date — колонка для поля TripDay.date
-- (специально не "date", чтобы не пересекаться с ключевым словом SQL).
CREATE TABLE trip_days (
    id          BIGSERIAL     PRIMARY KEY,
    trip_id     BIGINT        NOT NULL REFERENCES trips (id),
    trip_date   DATE,
    city        VARCHAR(255),
    description VARCHAR(2000)
);

CREATE INDEX idx_trip_days_trip_id ON trip_days (trip_id);

-- Расходы внутри дня.
CREATE TABLE expenses (
    id             BIGSERIAL     PRIMARY KEY,
    trip_day_id    BIGINT        NOT NULL REFERENCES trip_days (id),
    category       VARCHAR(50)   NOT NULL,
    description    VARCHAR(255),
    estimated_cost DECIMAL(10,2),
    actual_cost    DECIMAL(10,2)
);

CREATE INDEX idx_expenses_trip_day_id ON expenses (trip_day_id);
