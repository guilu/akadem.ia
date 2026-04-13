-- 1. Create syllabuses table
CREATE TABLE syllabuses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',
    owner_id    UUID,
    CONSTRAINT chk_syllabuses_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE')),
    CONSTRAINT fk_syllabuses_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_syllabuses_visibility ON syllabuses(visibility);
CREATE INDEX idx_syllabuses_owner_id   ON syllabuses(owner_id);

-- 2. Add syllabus_id to subjects (nullable first)
ALTER TABLE subjects ADD COLUMN syllabus_id UUID
    CONSTRAINT fk_subjects_syllabus REFERENCES syllabuses(id) ON DELETE RESTRICT;

-- 3. Insert one global "Default" temario
INSERT INTO syllabuses (id, name, description, visibility)
VALUES (gen_random_uuid(), 'Default', 'Temario por defecto', 'GLOBAL');

-- 4. Assign all existing subjects to the "Default" syllabus
UPDATE subjects
SET syllabus_id = (SELECT id FROM syllabuses WHERE name = 'Default' AND visibility = 'GLOBAL' LIMIT 1)
WHERE syllabus_id IS NULL;

-- 5. Enforce NOT NULL now that data is migrated
ALTER TABLE subjects ALTER COLUMN syllabus_id SET NOT NULL;

-- 6. Index for query performance
CREATE INDEX idx_subjects_syllabus_id ON subjects(syllabus_id);
