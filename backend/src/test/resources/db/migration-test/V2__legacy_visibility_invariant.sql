-- Mirror of V007__legacy_content_visibility_invariant.sql
-- Used by LegacyVisibilityMigrationTest via a dedicated Flyway instance.

-- Step 1: Normalize legacy GLOBAL rows (idempotent – documents intent)
UPDATE subjects   SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE units      SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE questions  SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE flashcards SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;

-- Step 2: Heal orphaned PRIVATE rows (no owner → promote to GLOBAL)
UPDATE subjects   SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE units      SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE questions  SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE flashcards SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;

-- Step 3: Add cross-column integrity constraints
ALTER TABLE subjects
    ADD CONSTRAINT chk_subjects_visibility_integrity
        CHECK ((visibility = 'GLOBAL'  AND owner_id IS NULL)
            OR (visibility = 'PRIVATE' AND owner_id IS NOT NULL));

ALTER TABLE units
    ADD CONSTRAINT chk_units_visibility_integrity
        CHECK ((visibility = 'GLOBAL'  AND owner_id IS NULL)
            OR (visibility = 'PRIVATE' AND owner_id IS NOT NULL));

ALTER TABLE questions
    ADD CONSTRAINT chk_questions_visibility_integrity
        CHECK ((visibility = 'GLOBAL'  AND owner_id IS NULL)
            OR (visibility = 'PRIVATE' AND owner_id IS NOT NULL));

ALTER TABLE flashcards
    ADD CONSTRAINT chk_flashcards_visibility_integrity
        CHECK ((visibility = 'GLOBAL'  AND owner_id IS NULL)
            OR (visibility = 'PRIVATE' AND owner_id IS NOT NULL));
