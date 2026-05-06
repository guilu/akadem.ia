-- subjects
ALTER TABLE subjects ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE subjects ADD COLUMN owner_id UUID;
ALTER TABLE subjects ADD CONSTRAINT chk_subjects_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'));
ALTER TABLE subjects ADD CONSTRAINT fk_subjects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_subjects_visibility ON subjects(visibility);
CREATE INDEX idx_subjects_owner_id ON subjects(owner_id);

-- units
ALTER TABLE units ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE units ADD COLUMN owner_id UUID;
ALTER TABLE units ADD CONSTRAINT chk_units_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'));
ALTER TABLE units ADD CONSTRAINT fk_units_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_units_visibility ON units(visibility);
CREATE INDEX idx_units_owner_id ON units(owner_id);

-- questions
ALTER TABLE questions ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE questions ADD COLUMN owner_id UUID;
ALTER TABLE questions ADD CONSTRAINT chk_questions_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'));
ALTER TABLE questions ADD CONSTRAINT fk_questions_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_questions_visibility ON questions(visibility);
CREATE INDEX idx_questions_owner_id ON questions(owner_id);

-- flashcards
ALTER TABLE flashcards ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE flashcards ADD COLUMN owner_id UUID;
ALTER TABLE flashcards ADD CONSTRAINT chk_flashcards_visibility CHECK (visibility IN ('GLOBAL', 'PRIVATE'));
ALTER TABLE flashcards ADD CONSTRAINT fk_flashcards_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_flashcards_visibility ON flashcards(visibility);
CREATE INDEX idx_flashcards_owner_id ON flashcards(owner_id);
