package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseTest {

  private final String email = "buyer@example.com";
  private final String productId = "TEMARIO_SUBALTERNO_GVA";
  private final String paymentIntentId = "pi_test_123";
  private final long amountCents = 1500L;
  private final String currency = "eur";

  // --- factory: Purchase.create(...) ---

  @Test
  void createBuildsPurchaseWithStatusPending() {
    UUID downloadToken = UUID.randomUUID();

    Purchase p = Purchase.create(email, productId, paymentIntentId, downloadToken, amountCents, currency);

    assertEquals(PurchaseStatus.PENDING, p.getStatus());
  }

  @Test
  void createGeneratesNonNullDownloadTokenWhenProvided() {
    UUID downloadToken = UUID.randomUUID();

    Purchase p = Purchase.create(email, productId, paymentIntentId, downloadToken, amountCents, currency);

    assertNotNull(p.getDownloadToken());
    assertEquals(downloadToken, p.getDownloadToken());
  }

  @Test
  void createPopulatesAllRequiredFields() {
    UUID downloadToken = UUID.randomUUID();

    Purchase p = Purchase.create(email, productId, paymentIntentId, downloadToken, amountCents, currency);

    assertNotNull(p.getId());
    assertEquals(email, p.getEmail());
    assertEquals(productId, p.getProductId());
    assertEquals(paymentIntentId, p.getStripePaymentIntentId());
    assertEquals(amountCents, p.getAmountCents());
    assertEquals(currency, p.getCurrency());
    assertNotNull(p.getCreatedAt());
    assertNull(p.getPaidAt());
    assertNull(p.getEmailSentAt());
    assertNull(p.getUserId());
  }

  // --- markEmailSent ---

  @Test
  void markEmailSentSetsEmailSentAt() {
    UUID downloadToken = UUID.randomUUID();
    Purchase p = Purchase.create(email, productId, paymentIntentId, downloadToken, amountCents, currency);
    Instant when = Instant.parse("2026-04-27T10:15:30Z");

    p.markEmailSent(when);

    assertEquals(when, p.getEmailSentAt());
  }

  @Test
  void markEmailSentNullInstantThrows() {
    UUID downloadToken = UUID.randomUUID();
    Purchase p = Purchase.create(email, productId, paymentIntentId, downloadToken, amountCents, currency);

    assertThrows(IllegalArgumentException.class, () -> p.markEmailSent(null));
  }
}
