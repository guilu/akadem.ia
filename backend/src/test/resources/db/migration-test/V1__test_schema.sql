-- Minimal schema for LegacyVisibilityMigrationTest
-- Creates the 4 tables that V007 operates on, with the columns added by V006.
-- Intentionally leaves out tables unrelated to the visibility migration
-- (pgcrypto extension is H2-incompatible so the full Flyway chain is not used).

CREATE TABLE IF NOT EXISTS users (
    id   UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    role  VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS subjects (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',
    owner_id    UUID,
    CONSTRAINT fk_subjects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_subjects_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'))
);

CREATE TABLE IF NOT EXISTS units (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    subject_id  UUID         NOT NULL,
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',
    owner_id    UUID,
    CONSTRAINT fk_units_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_units_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'))
);

CREATE TABLE IF NOT EXISTS questions (
    id          UUID PRIMARY KEY,
    unit_id     UUID NOT NULL,
    text        TEXT NOT NULL,
    visibility  VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    owner_id    UUID,
    CONSTRAINT fk_questions_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_questions_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'))
);

CREATE TABLE IF NOT EXISTS flashcards (
    id          UUID PRIMARY KEY,
    unit_id     UUID NOT NULL,
    front       TEXT NOT NULL,
    back        TEXT NOT NULL,
    visibility  VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
    owner_id    UUID,
    CONSTRAINT fk_flashcards_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_flashcards_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'))
);
