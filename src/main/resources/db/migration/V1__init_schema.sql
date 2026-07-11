CREATE TABLE activities (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    detail     TEXT,
    icon_id    BIGINT
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    avatar_id     BIGINT,
    bio           VARCHAR(500),
    age_years     INTEGER,
    height_cm     INTEGER,
    weight_kg     DOUBLE PRECISION,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);

CREATE TABLE user_activity_ids (
    user_id     BIGINT NOT NULL REFERENCES users (id),
    activity_id BIGINT NOT NULL REFERENCES activities (id)
);

CREATE TABLE images (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT       NOT NULL,
    external_id  VARCHAR(500) NOT NULL,
    url          VARCHAR(1000),
    status       VARCHAR(10)  NOT NULL,
    upload_token VARCHAR(36) UNIQUE,
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_images_owner ON images (owner_id);
CREATE INDEX idx_images_upload_token ON images (upload_token);
CREATE INDEX idx_images_status ON images (status);

CREATE TABLE videos (
    id                BIGSERIAL PRIMARY KEY,
    owner_id          BIGINT       NOT NULL,
    external_id       VARCHAR(500) NOT NULL,
    url               VARCHAR(1000),
    thumbnail_url     VARCHAR(1000),
    duration_seconds  INTEGER,
    status            VARCHAR(10)  NOT NULL,
    upload_token      VARCHAR(36) UNIQUE,
    expires_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_videos_owner ON videos (owner_id);
CREATE INDEX idx_videos_upload_token ON videos (upload_token);
CREATE INDEX idx_videos_status ON videos (status);