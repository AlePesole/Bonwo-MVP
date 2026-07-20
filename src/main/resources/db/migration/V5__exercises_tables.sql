CREATE TABLE exercises (
    id                   BIGSERIAL PRIMARY KEY,
    owner_id             BIGINT       NOT NULL,
    title                VARCHAR(200) NOT NULL,
    level                VARCHAR(20)  NOT NULL,
    thumbnail_id         BIGINT,
    main_video_id        BIGINT,
    description          TEXT,
    instructions         TEXT,
    muscle_summary_json  TEXT,
    created_at           TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_exercises_owner ON exercises (owner_id);

CREATE TABLE exercise_muscles (
    exercise_id   BIGINT NOT NULL REFERENCES exercises (id),
    sub_group_id  BIGINT NOT NULL,
    activation    DOUBLE PRECISION NOT NULL
);

CREATE TABLE exercise_equipment (
    exercise_id   BIGINT NOT NULL REFERENCES exercises (id),
    equipment_id  BIGINT NOT NULL
);

CREATE TABLE exercise_activities (
    exercise_id  BIGINT NOT NULL REFERENCES exercises (id),
    activity_id  BIGINT NOT NULL
);

CREATE TABLE exercise_training_goals (
    exercise_id       BIGINT NOT NULL REFERENCES exercises (id),
    training_goal_id  BIGINT NOT NULL
);