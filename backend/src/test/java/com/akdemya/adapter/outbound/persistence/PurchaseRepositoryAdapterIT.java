package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.PurchaseMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaPurchaseRepository;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link PurchaseRepositoryAdapter}.
 *
 * <p>Uses {@code @DataJpaTest} with H2 in PostgreSQL compatibility mode — the project's
 * standard pattern (no Testcontainers). All scenarios required by Phase 6.4 of
 * {@code descarga-temario-stripe} are exercised:
 * round-trip persistence, idempotency of {@code markPaid} / {@code markFailed},
 * filtering of {@code findPendingOlderThan} and {@code findPaidWithoutEmail},
 * lookups by download token / payment intent id, and {@code updateEmailSentAt}.</p>
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(PurchaseMapper.class)
class PurchaseRepositoryAdapterIT {

  @Autowired
  JpaPurchaseRepository jpa;

  @Autowired
  PurchaseMapper mapper;

  private PurchaseRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new PurchaseRepositoryAdapter(jpa, mapper);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // save round-trip
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void save_persistsAllFieldsRoundTripped() {
    Purchase original = Purchase.create(
        "buyer@example.com",
        "TEMARIO_SUBALTERNO_GVA",
        "pi_test_save_001",
        UUID.randomUUID(),
        1500L,
        "eur"
    );

    Purchase saved = adapter.save(original);

    assertEquals(original.getId(), saved.getId());

    Optional<Purchase> reloaded = adapter.findByDownloadToken(original.getDownloadToken());
    assertTrue(reloaded.isPresent());
    Purchase r = reloaded.get();
    assertEquals(original.getId(), r.getId());
    assertEquals("pi_test_save_001", r.getStripePaymentIntentId());
    assertEquals(original.getDownloadToken(), r.getDownloadToken());
    assertEquals("buyer@example.com", r.getEmail());
    assertEquals("TEMARIO_SUBALTERNO_GVA", r.getProductId());
    assertNull(r.getUserId());
    assertEquals(PurchaseStatus.PENDING, r.getStatus());
    assertEquals(1500L, r.getAmountCents());
    assertEquals("eur", r.getCurrency());
    assertNotNull(r.getCreatedAt());
    assertNull(r.getPaidAt());
    assertNull(r.getEmailSentAt());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // markPaid — idempotency
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void markPaid_returns1FirstTimeAnd0SecondTime() {
    String piId = "pi_test_paid_001";
    adapter.save(Purchase.create("a@x.com", "P1", piId, UUID.randomUUID(), 1000L, "eur"));

    int firstAffected = adapter.markPaid(piId, Instant.now());
    int secondAffected = adapter.markPaid(piId, Instant.now());

    assertEquals(1, firstAffected, "First markPaid should update the PENDING row");
    assertEquals(0, secondAffected, "Second markPaid is idempotent — no PENDING row left");

    Optional<Purchase> reloaded = adapter.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals(PurchaseStatus.PAID, reloaded.get().getStatus());
    assertNotNull(reloaded.get().getPaidAt());
  }

  @Test
  void markPaid_returns0WhenAlreadyFailed() {
    String piId = "pi_test_paid_after_fail";
    adapter.save(Purchase.create("a@x.com", "P1", piId, UUID.randomUUID(), 1000L, "eur"));

    int failed = adapter.markFailed(piId);
    int paid = adapter.markPaid(piId, Instant.now());

    assertEquals(1, failed);
    assertEquals(0, paid, "markPaid must NOT overwrite a FAILED row");

    Optional<Purchase> reloaded = adapter.findByStripePaymentIntentId(piId);
    assertEquals(PurchaseStatus.FAILED, reloaded.orElseThrow().getStatus());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // markFailed — idempotency
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void markFailed_returns1FirstTimeAnd0SecondTime() {
    String piId = "pi_test_failed_001";
    adapter.save(Purchase.create("a@x.com", "P1", piId, UUID.randomUUID(), 1000L, "eur"));

    int firstAffected = adapter.markFailed(piId);
    int secondAffected = adapter.markFailed(piId);

    assertEquals(1, firstAffected);
    assertEquals(0, secondAffected, "Second markFailed is idempotent");

    Optional<Purchase> reloaded = adapter.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals(PurchaseStatus.FAILED, reloaded.get().getStatus());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // findPendingOlderThan
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void findPendingOlderThan_returnsOnlyOldPendingRows() {
    Instant now = Instant.now();
    Instant cutoff = now.minus(30, ChronoUnit.MINUTES);

    // 1. PENDING + old → should match
    Purchase oldPending = adapter.save(Purchase.create(
        "old@x.com", "P1", "pi_old_pending", UUID.randomUUID(), 1000L, "eur"));
    backdateCreatedAt(oldPending.getId(), now.minus(2, ChronoUnit.HOURS));

    // 2. PENDING + recent → should NOT match
    adapter.save(Purchase.create(
        "recent@x.com", "P1", "pi_recent_pending", UUID.randomUUID(), 1000L, "eur"));

    // 3. PAID + old → should NOT match
    Purchase oldPaid = adapter.save(Purchase.create(
        "paid@x.com", "P1", "pi_old_paid", UUID.randomUUID(), 1000L, "eur"));
    backdateCreatedAt(oldPaid.getId(), now.minus(2, ChronoUnit.HOURS));
    adapter.markPaid("pi_old_paid", now);

    List<Purchase> result = adapter.findPendingOlderThan(cutoff);

    assertEquals(1, result.size());
    assertEquals(oldPending.getId(), result.get(0).getId());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // findPaidWithoutEmail — respects grace cutoff
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void findPaidWithoutEmail_filtersByGraceCutoffAndAlreadyEmailed() {
    Instant now = Instant.now();
    Instant graceCutoff = now.minus(5, ChronoUnit.MINUTES);

    // 1. PAID + paid_at old + email_sent_at NULL → MATCH
    Purchase oldPaidNoEmail = adapter.save(Purchase.create(
        "old@x.com", "P1", "pi_old_paid_no_email", UUID.randomUUID(), 1000L, "eur"));
    adapter.markPaid("pi_old_paid_no_email", now.minus(1, ChronoUnit.HOURS));

    // 2. PAID + paid_at recent (inside grace window) → no match
    Purchase recentPaid = adapter.save(Purchase.create(
        "recent@x.com", "P1", "pi_recent_paid", UUID.randomUUID(), 1000L, "eur"));
    adapter.markPaid("pi_recent_paid", now.minus(1, ChronoUnit.MINUTES));

    // 3. PAID + paid_at old + already emailed → no match
    Purchase paidEmailed = adapter.save(Purchase.create(
        "emailed@x.com", "P1", "pi_paid_emailed", UUID.randomUUID(), 1000L, "eur"));
    adapter.markPaid("pi_paid_emailed", now.minus(1, ChronoUnit.HOURS));
    adapter.updateEmailSentAt(paidEmailed.getId(), now.minus(30, ChronoUnit.MINUTES));

    // 4. PENDING + old → no match (status filter)
    Purchase pending = adapter.save(Purchase.create(
        "pend@x.com", "P1", "pi_old_pending2", UUID.randomUUID(), 1000L, "eur"));
    backdateCreatedAt(pending.getId(), now.minus(2, ChronoUnit.HOURS));

    List<Purchase> result = adapter.findPaidWithoutEmail(graceCutoff);

    assertEquals(1, result.size());
    assertEquals(oldPaidNoEmail.getId(), result.get(0).getId());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // findByDownloadToken / findByStripePaymentIntentId — Optional contract
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void findByDownloadToken_returnsEmptyForUnknownToken() {
    assertTrue(adapter.findByDownloadToken(UUID.randomUUID()).isEmpty());
  }

  @Test
  void findByStripePaymentIntentId_returnsEmptyForUnknownPi() {
    assertTrue(adapter.findByStripePaymentIntentId("pi_does_not_exist").isEmpty());
  }

  @Test
  void findByDownloadToken_returnsPurchaseWhenPresent() {
    UUID token = UUID.randomUUID();
    Purchase saved = adapter.save(Purchase.create(
        "a@x.com", "P1", "pi_lookup_token", token, 1000L, "eur"));

    Optional<Purchase> result = adapter.findByDownloadToken(token);

    assertTrue(result.isPresent());
    assertEquals(saved.getId(), result.get().getId());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // updateEmailSentAt
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void updateEmailSentAt_setsTimestampOnRow() {
    Purchase saved = adapter.save(Purchase.create(
        "a@x.com", "P1", "pi_email_sent", UUID.randomUUID(), 1000L, "eur"));
    Instant when = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    adapter.updateEmailSentAt(saved.getId(), when);

    Purchase reloaded = adapter.findByDownloadToken(saved.getDownloadToken()).orElseThrow();
    assertNotNull(reloaded.getEmailSentAt());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Backdates the {@code created_at} column for a saved purchase. Required because
   * {@link Purchase#create} always sets {@code createdAt = Instant.now()}, but the
   * {@code findPendingOlderThan} test needs rows with an explicit older timestamp.
   */
  private void backdateCreatedAt(UUID purchaseId, Instant when) {
    var entity = jpa.findById(purchaseId).orElseThrow();
    entity.setCreatedAt(when);
    jpa.saveAndFlush(entity);
  }
}
