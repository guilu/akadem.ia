-- ============================================================
-- V007: Legacy content visibility classification
-- Business decision: all pre-existing content (created by admins
-- before user self-service existed) is classified as GLOBAL.
-- GLOBAL rows must have owner_id = NULL.
-- PRIVATE rows must have owner_id IS NOT NULL.
-- ============================================================

-- Step 1: Normalize legacy GLOBAL rows (idempotent – documents intent)
UPDATE subjects   SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE units      SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE questions  SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;
UPDATE flashcards SET visibility = 'GLOBAL', owner_id = NULL WHERE visibility = 'GLOBAL' AND owner_id IS NULL;

-- Step 2: Heal orphaned PRIVATE rows (no owner → promote to GLOBAL)
-- Rationale: any row that ended up PRIVATE without an owner_id is
-- inconsistent; safest resolution is to treat it as shared content.
UPDATE subjects   SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE units      SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE questions  SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;
UPDATE flashcards SET visibility = 'GLOBAL' WHERE visibility = 'PRIVATE' AND owner_id IS NULL;

-- Step 3: Add cross-column integrity constraints
-- Invariant: GLOBAL <=> owner_id IS NULL; PRIVATE <=> owner_id IS NOT NULL
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
