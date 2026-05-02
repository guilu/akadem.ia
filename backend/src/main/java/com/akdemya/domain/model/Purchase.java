package com.akdemya.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate that tracks a Stripe payment for a digital product purchase.
 *
 * <p>Created in {@link PurchaseStatus#PENDING} status when a PaymentIntent is created;
 * transitions to {@code PAID} when the corresponding webhook is received (or the
 * reconciliation scheduler observes the PaymentIntent succeeded), or to {@code FAILED}
 * when the payment is canceled or fails.</p>
 *
 * <p>Domain model — pure Java, no framework dependencies. Mutable only via explicit
 * domain operations ({@link #markEmailSent(Instant)}); persistence transitions
 * (PENDING → PAID/FAILED) happen at the repository layer via atomic UPDATE statements.</p>
 */
public class Purchase {

  private final UUID id;
  private final String stripePaymentIntentId;
  private final UUID downloadToken;
  private final String email;
  private final String productId;
  private final UUID userId;
  private final PurchaseStatus status;
  private final long amountCents;
  private final String currency;
  private final Instant createdAt;
  private final Instant paidAt;
  private Instant emailSentAt;

  public Purchase(UUID id,
                  String stripePaymentIntentId,
                  UUID downloadToken,
                  String email,
                  String productId,
                  UUID userId,
                  PurchaseStatus status,
                  long amountCents,
                  String currency,
                  Instant createdAt,
                  Instant paidAt,
                  Instant emailSentAt) {
    if (id == null) throw new IllegalArgumentException("id cannot be null");
    if (stripePaymentIntentId == null || stripePaymentIntentId.trim().isEmpty())
      throw new IllegalArgumentException("stripePaymentIntentId cannot be blank");
    if (downloadToken == null) throw new IllegalArgumentException("downloadToken cannot be null");
    if (email == null || email.trim().isEmpty())
      throw new IllegalArgumentException("email cannot be blank");
    if (productId == null || productId.trim().isEmpty())
      throw new IllegalArgumentException("productId cannot be blank");
    if (status == null) throw new IllegalArgumentException("status cannot be null");
    if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");
    if (currency == null || currency.trim().isEmpty())
      throw new IllegalArgumentException("currency cannot be blank");
    if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
    this.id = id;
    this.stripePaymentIntentId = stripePaymentIntentId;
    this.downloadToken = downloadToken;
    this.email = email;
    this.productId = productId;
    this.userId = userId;
    this.status = status;
    this.amountCents = amountCents;
    this.currency = currency;
    this.createdAt = createdAt;
    this.paidAt = paidAt;
    this.emailSentAt = emailSentAt;
  }

  /**
   * Factory for a brand-new purchase in {@link PurchaseStatus#PENDING} status.
   * The {@code paidAt} and {@code emailSentAt} fields are null until the payment
   * succeeds and the download email is delivered, respectively.
   */
  public static Purchase create(String email,
                                String productId,
                                String stripePaymentIntentId,
                                UUID downloadToken,
                                long amountCents,
                                String currency) {
    return new Purchase(
        UUID.randomUUID(),
        stripePaymentIntentId,
        downloadToken,
        email,
        productId,
        null,
        PurchaseStatus.PENDING,
        amountCents,
        currency,
        Instant.now(),
        null,
        null
    );
  }

  /** Records the timestamp at which the download email was successfully delivered. */
  public void markEmailSent(Instant when) {
    if (when == null) throw new IllegalArgumentException("emailSentAt cannot be null");
    this.emailSentAt = when;
  }

  public UUID getId() { return id; }
  public String getStripePaymentIntentId() { return stripePaymentIntentId; }
  public UUID getDownloadToken() { return downloadToken; }
  public String getEmail() { return email; }
  public String getProductId() { return productId; }
  public UUID getUserId() { return userId; }
  public PurchaseStatus getStatus() { return status; }
  public long getAmountCents() { return amountCents; }
  public String getCurrency() { return currency; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getPaidAt() { return paidAt; }
  public Instant getEmailSentAt() { return emailSentAt; }
}
