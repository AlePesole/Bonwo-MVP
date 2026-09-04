CREATE TABLE exercise_publications (
    id            BIGSERIAL PRIMARY KEY,
    exercise_id   BIGINT      NOT NULL REFERENCES exercises (id),
    author_id     BIGINT      NOT NULL,
    type          VARCHAR(20) NOT NULL,
    visibility    VARCHAR(20) NOT NULL,
    likes_count   BIGINT      NOT NULL DEFAULT 0,
    saves_count   BIGINT      NOT NULL DEFAULT 0,
    views_count   BIGINT      NOT NULL DEFAULT 0,
    uses_count    BIGINT      NOT NULL DEFAULT 0,
    published_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_exercise_publications_exercise ON exercise_publications (exercise_id);
CREATE INDEX idx_exercise_publications_author ON exercise_publications (author_id);

ALTER TABLE exercises ADD COLUMN publication_id BIGINT REFERENCES exercise_publications (id);
CREATE INDEX idx_exercises_publication ON exercises (publication_id);

-- Likes/saves toggle per user; views/uses are insert-only and never removed.

CREATE TABLE exercise_publication_likes (
    id             BIGSERIAL PRIMARY KEY,
    publication_id BIGINT      NOT NULL REFERENCES exercise_publications (id),
    user_id        BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_publication_like_user UNIQUE (publication_id, user_id)
);

CREATE TABLE exercise_publication_saves (
    id             BIGSERIAL PRIMARY KEY,
    publication_id BIGINT      NOT NULL REFERENCES exercise_publications (id),
    user_id        BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_publication_save_user UNIQUE (publication_id, user_id)
);

CREATE TABLE exercise_publication_views (
    id             BIGSERIAL PRIMARY KEY,
    publication_id BIGINT      NOT NULL REFERENCES exercise_publications (id),
    user_id        BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_publication_view_user UNIQUE (publication_id, user_id)
);

-- usesCount increments only the first time a given user's routine uses the exercise — routine_id
-- is stored for context only
CREATE TABLE exercise_publication_uses (
    id             BIGSERIAL PRIMARY KEY,
    publication_id BIGINT      NOT NULL REFERENCES exercise_publications (id),
    user_id        BIGINT      NOT NULL,
    routine_id     BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_publication_use_user UNIQUE (publication_id, user_id)
);
