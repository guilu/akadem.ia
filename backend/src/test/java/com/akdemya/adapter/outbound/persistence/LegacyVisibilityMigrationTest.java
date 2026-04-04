package com.akdemya.adapter.outbound.persistence;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates the V007 legacy visibility migration.
 *
 * <p>Uses an in-memory H2 database with a dedicated Flyway migration path
 * ({@code db/migration-test}) that mirrors the production V007 SQL.
 * This avoids the full Flyway chain (which requires pgcrypto / real Postgres)
 * while still exercising every UPDATE and CHECK constraint defined in V007.
 *
 * <p>Setup strategy:
 * <ol>
 *   <li>Run Flyway up to V1 only (creates tables, no integrity constraint yet).</li>
 *   <li>Insert test data directly — the V1 schema has no cross-column constraint,
 *       so inserting PRIVATE + NULL owner_id is valid at this stage.</li>
 *   <li>Run Flyway V2 (the migration under test) on top of the seeded data.</li>
 *   <li>Assert expected post-migration state.</li>
 * </ol>
 */
class LegacyVisibilityMigrationTest {

    private static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT_ID  = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID UNIT_GLOBAL  = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID UNIT_ORPHAN  = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID UNIT_PRIVATE = UUID.fromString("10000000-0000-0000-0000-000000000003");

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        // Unique DB name per test run ensures full isolation
        String dbName = "migtest_" + UUID.randomUUID().toString().replace("-", "");
        ds.setURL("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUser("sa");
        ds.setPassword("");

        this.jdbc = new JdbcTemplate(ds);

        // Phase 1: apply V1 only (table DDL, no cross-column constraint yet)
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration-test")
                .target("1")
                .load()
                .migrate();

        // Phase 2: seed legacy / orphaned / valid data
        seedTestData();

        // Phase 3: apply V2 (the migration under test: normalize + heal + add constraints)
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration-test")
                .load()
                .migrate();
    }

    /**
     * Seeds data that represents the real-world state BEFORE V007.
     * The V1 schema has no cross-column integrity constraint, so
     * PRIVATE + NULL owner_id is a valid insert at this stage.
     */
    private void seedTestData() {
        // Reference user (FK target for valid PRIVATE rows)
        jdbc.update("INSERT INTO users (id, email, role) VALUES (?, ?, ?)",
                OWNER_ID, "owner@test.com", "USER");

        // Parent subject
        jdbc.update("INSERT INTO subjects (id, name, visibility, owner_id) VALUES (?, ?, 'GLOBAL', NULL)",
                SUBJECT_ID, "Legacy Subject");

        // --- units ---
        // (1) Legacy GLOBAL row – must survive unchanged
        jdbc.update("INSERT INTO units (id, name, subject_id, visibility, owner_id) VALUES (?, ?, ?, 'GLOBAL', NULL)",
                UNIT_GLOBAL, "Global Unit", SUBJECT_ID);

        // (2) Orphaned PRIVATE row (owner_id IS NULL) – must be healed to GLOBAL by V2
        jdbc.update("INSERT INTO units (id, name, subject_id, visibility, owner_id) VALUES (?, ?, ?, 'PRIVATE', NULL)",
                UNIT_ORPHAN, "Orphaned Unit", SUBJECT_ID);

        // (3) Valid PRIVATE row with owner – must remain PRIVATE after V2
        jdbc.update("INSERT INTO units (id, name, subject_id, visibility, owner_id) VALUES (?, ?, ?, 'PRIVATE', ?)",
                UNIT_PRIVATE, "Private Unit", SUBJECT_ID, OWNER_ID);

        // --- questions (same three patterns) ---
        jdbc.update("INSERT INTO questions (id, unit_id, text, visibility, owner_id) VALUES (?, ?, ?, 'GLOBAL', NULL)",
                UUID.fromString("20000000-0000-0000-0000-000000000001"), UNIT_GLOBAL, "Global Q");
        jdbc.update("INSERT INTO questions (id, unit_id, text, visibility, owner_id) VALUES (?, ?, ?, 'PRIVATE', NULL)",
                UUID.fromString("20000000-0000-0000-0000-000000000002"), UNIT_GLOBAL, "Orphaned Q");
        jdbc.update("INSERT INTO questions (id, unit_id, text, visibility, owner_id) VALUES (?, ?, ?, 'PRIVATE', ?)",
                UUID.fromString("20000000-0000-0000-0000-000000000003"), UNIT_GLOBAL, "Private Q", OWNER_ID);

        // --- flashcards (same three patterns) ---
        jdbc.update("INSERT INTO flashcards (id, unit_id, front, back, visibility, owner_id) VALUES (?, ?, ?, ?, 'GLOBAL', NULL)",
                UUID.fromString("30000000-0000-0000-0000-000000000001"), UNIT_GLOBAL, "F1", "B1");
        jdbc.update("INSERT INTO flashcards (id, unit_id, front, back, visibility, owner_id) VALUES (?, ?, ?, ?, 'PRIVATE', NULL)",
                UUID.fromString("30000000-0000-0000-0000-000000000002"), UNIT_GLOBAL, "F2", "B2");
        jdbc.update("INSERT INTO flashcards (id, unit_id, front, back, visibility, owner_id) VALUES (?, ?, ?, ?, 'PRIVATE', ?)",
                UUID.fromString("30000000-0000-0000-0000-000000000003"), UNIT_GLOBAL, "F3", "B3", OWNER_ID);
    }

    // ---------------------------------------------------------------------------
    // Test scenarios
    // ---------------------------------------------------------------------------

    @Test
    void legacyGlobalRowSurvivesUnchanged() {
        String visibility = jdbc.queryForObject(
                "SELECT visibility FROM units WHERE id = ?", String.class, UNIT_GLOBAL);
        String ownerId = jdbc.queryForObject(
                "SELECT CAST(owner_id AS VARCHAR) FROM units WHERE id = ?", String.class, UNIT_GLOBAL);

        assertEquals("GLOBAL", visibility);
        assertNull(ownerId);
    }

    @Test
    void orphanedPrivateRowIsHealedToGlobal() {
        String visibility = jdbc.queryForObject(
                "SELECT visibility FROM units WHERE id = ?", String.class, UNIT_ORPHAN);

        assertEquals("GLOBAL", visibility, "Orphaned PRIVATE row must be promoted to GLOBAL by V007");
    }

    @Test
    void validPrivateRowWithOwnerIsUntouched() {
        String visibility = jdbc.queryForObject(
                "SELECT visibility FROM units WHERE id = ?", String.class, UNIT_PRIVATE);

        assertEquals("PRIVATE", visibility, "Valid PRIVATE row with owner_id must remain PRIVATE after V007");
    }

    @Test
    void insertPrivateWithNullOwnerThrowsConstraintViolation() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update(
                        "INSERT INTO units (id, name, subject_id, visibility, owner_id) VALUES (?, ?, ?, 'PRIVATE', NULL)",
                        UUID.randomUUID(), "Bad Private", SUBJECT_ID
                )
        );
    }

    @Test
    void insertGlobalWithNonNullOwnerThrowsConstraintViolation() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update(
                        "INSERT INTO units (id, name, subject_id, visibility, owner_id) VALUES (?, ?, ?, 'GLOBAL', ?)",
                        UUID.randomUUID(), "Bad Global", SUBJECT_ID, OWNER_ID
                )
        );
    }

    @Test
    void zeroOrphanedRecordsRemainAfterMigration() {
        for (String table : new String[]{"subjects", "units", "questions", "flashcards"}) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE visibility = 'PRIVATE' AND owner_id IS NULL",
                    Integer.class
            );
            assertEquals(0, count,
                    "Table [" + table + "] must have zero orphaned PRIVATE rows after V007 migration");
        }
    }
}
