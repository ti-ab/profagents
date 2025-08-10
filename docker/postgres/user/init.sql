create schema if not exists users;
set search_path to users;

CREATE TABLE IF NOT EXISTS progress (
    id BIGSERIAL PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    book_id         BIGINT          NOT NULL,
    chapter_id      BIGINT,
    subchapter_id   BIGINT,
    section_id      BIGINT,
    section_rating  DOUBLE PRECISION,
    section_time    BIGINT,                 -- secondes passées sur la section
    start_timestamp TIMESTAMP WITH TIME ZONE,
    end_timestamp   TIMESTAMP WITH TIME ZONE
);

