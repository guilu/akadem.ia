package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code purchases} table (see {@code V011__purchases.sql}).
 *
 * <p>Columns:
 * <ul>
 *   <li>{@code id} — UUID PK</li>
 *   <li>{@code stripe_payment_intent_id} — unique, used by webhook lookups</li>
 *   <li>{@code download_token} — unique, used by GET /api/v1/downloads/{token}</li>
 *   <li>{@code email} — buyer email</li>
 *   <li>{@code product_id} — catalog SKU</li>
 *   <li>{@code user_id} — nullable; only set when the buyer was logged in</li>
 *   <li>{@code status} — {@code PENDING} | {@code PAID} | {@code FAILED} (DB CHECK constraint)</li>
 *   <li>{@code amount_cents} — captured amount in minor units</li>
 *   <li>{@code currency} — ISO currency code (lowercase as Stripe returns it)</li>
 *   <li>{@code created_at} — set on insert</li>
 *   <li>{@code paid_at} — set when webhook flips status to PAID</li>
 *   <li>{@code email_sent_at} — set when the download email is delivered</li>
 * </ul>
 */
@Entity
@Table(name = "purchases")
public class PurchaseEntity {

  @Id
  private UUID id;

  @Column(name = "stripe_payment_intent_id", nullable = false, length = 255)
  private String stripePaymentIntentId;

  @Column(name = "download_token", nullable = false)
  private UUID downloadToken;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(name = "product_id", nullable = false, length = 100)
  private String productId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(nullable = false, length = 10)
  private String currency;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "email_sent_at")
  private Instant emailSentAt;

  public PurchaseEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getStripePaymentIntentId() { return stripePaymentIntentId; }
  public void setStripePaymentIntentId(String stripePaymentIntentId) {
    this.stripePaymentIntentId = stripePaymentIntentId;
  }

  public UUID getDownloadToken() { return downloadToken; }
  public void setDownloadToken(UUID downloadToken) { this.downloadToken = downloadToken; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getProductId() { return productId; }
  public void setProductId(String productId) { this.productId = productId; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public long getAmountCents() { return amountCents; }
  public void setAmountCents(long amountCents) { this.amountCents = amountCents; }

  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getPaidAt() { return paidAt; }
  public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

  public Instant getEmailSentAt() { return emailSentAt; }
  public void setEmailSentAt(Instant emailSentAt) { this.emailSentAt = emailSentAt; }
}
