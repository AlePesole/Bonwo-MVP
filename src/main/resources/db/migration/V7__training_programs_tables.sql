CREATE TABLE training_programs (
    id                  BIGSERIAL PRIMARY KEY,
    owner_id            BIGINT       NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    level               VARCHAR(20)  NOT NULL,
    thumbnail_id        BIGINT,
    days_per_week       INTEGER      NOT NULL,
    muscle_summary_json TEXT,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_training_programs_owner ON training_programs (owner_id);

CREATE TABLE training_program_equipment (
    training_program_id  BIGINT NOT NULL REFERENCES training_programs (id),
    equipment_id          BIGINT NOT NULL
);

CREATE TABLE training_program_activities (
    training_program_id  BIGINT NOT NULL REFERENCES training_programs (id),
    activity_id           BIGINT NOT NULL
);

CREATE TABLE training_program_training_goals (
    training_program_id  BIGINT NOT NULL REFERENCES training_programs (id),
    training_goal_id      BIGINT NOT NULL
);

-- A Routine can belong to a TrainingProgram's aggregate (real, independently-addressable Routine row,
-- not a copy) — null means it's a standalone routine owned directly by the user.
ALTER TABLE routines ADD COLUMN training_program_id BIGINT REFERENCES training_programs (id);
ALTER TABLE routines ADD COLUMN position INTEGER;

CREATE INDEX idx_routines_training_program ON routines (training_program_id);
