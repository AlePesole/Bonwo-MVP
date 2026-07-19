CREATE TABLE muscle_groups (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    icon_id    BIGINT
);

CREATE TABLE muscle_sub_groups (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT NOT NULL REFERENCES muscle_groups(id),
    name            VARCHAR(100) NOT NULL,
    detail          TEXT,
    svg_path_front  TEXT,
    svg_path_back   TEXT,
    icon_id         BIGINT,
    CONSTRAINT uq_muscle_sub_group_group_name UNIQUE (group_id, name),
    CONSTRAINT chk_muscle_sub_group_svg_path CHECK (svg_path_front IS NOT NULL OR svg_path_back IS NOT NULL)
);