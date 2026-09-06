CREATE TABLE training_sessions (
    id                   BIGSERIAL PRIMARY KEY,
    owner_id             BIGINT       NOT NULL,
    routine_id           BIGINT       REFERENCES routines (id) ON DELETE SET NULL,
    routine_title        VARCHAR(200) NOT NULL,
    training_program_id  BIGINT       REFERENCES training_programs (id) ON DELETE SET NULL,
    status               VARCHAR(20)  NOT NULL,
    started_at           TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ,
    duration             NUMERIC,
    final_note           TEXT,
    slots_json           TEXT         NOT NULL,
    muscle_summary_json  TEXT
);

CREATE INDEX idx_training_sessions_owner ON training_sessions (owner_id);
CREATE INDEX idx_training_sessions_owner_status ON training_sessions (owner_id, status);

-- Enforces "one IN_PROGRESS session per owner" at the DB level, closing the race window
-- the application-level check in TrainingSessionService.start() can't cover on its own.
CREATE UNIQUE INDEX uq_training_sessions_owner_in_progress
    ON training_sessions (owner_id)
    WHERE status = 'IN_PROGRESS';
