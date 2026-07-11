CREATE TABLE training_goals (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    detail     TEXT,
    icon_id    BIGINT
);

CREATE TABLE equipment (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    icon_id    BIGINT
);