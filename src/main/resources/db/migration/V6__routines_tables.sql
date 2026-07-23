CREATE TABLE routines (
    id                     BIGSERIAL PRIMARY KEY,
    owner_id               BIGINT       NOT NULL,
    title                  VARCHAR(200) NOT NULL,
    description            TEXT,
    level                  VARCHAR(20)  NOT NULL,
    thumbnail_id           BIGINT,
    estimated_duration     NUMERIC      NOT NULL,
    rest_between_exercises NUMERIC,
    muscle_summary_json    TEXT,
    created_at             TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_routines_owner ON routines (owner_id);

CREATE TABLE routine_slots (
    id                BIGSERIAL PRIMARY KEY,
    routine_id        BIGINT  NOT NULL REFERENCES routines (id),
    exercise_id       BIGINT  NOT NULL,
    position          INTEGER NOT NULL,
    rest_between_sets NUMERIC
);

CREATE INDEX idx_routine_slots_routine ON routine_slots (routine_id);

CREATE TABLE routine_slot_sets (
    slot_id     BIGINT      NOT NULL REFERENCES routine_slots (id),
    set_order   INTEGER     NOT NULL,
    set_type    VARCHAR(20) NOT NULL,
    reps        INTEGER     NOT NULL,
    weight_kg   DOUBLE PRECISION,
    weight_mode VARCHAR(20),
    duration    NUMERIC
);

CREATE TABLE routine_equipment (
    routine_id    BIGINT NOT NULL REFERENCES routines (id),
    equipment_id  BIGINT NOT NULL
);

CREATE TABLE routine_activities (
    routine_id   BIGINT NOT NULL REFERENCES routines (id),
    activity_id  BIGINT NOT NULL
);

CREATE TABLE routine_training_goals (
    routine_id        BIGINT NOT NULL REFERENCES routines (id),
    training_goal_id  BIGINT NOT NULL
);
