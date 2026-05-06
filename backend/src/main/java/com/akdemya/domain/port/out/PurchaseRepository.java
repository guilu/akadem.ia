package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Purchase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the {@link Purchase} aggregate.
 *
 * <p>Status transitions ({@code PENDING → PAID/FAILED}) are expressed as atomic
 * UPDATE operations returning the number of affected rows, so the application
 * layer can detect idempotent retries (a second webhook delivery for an already
 * PAID purchase returns {@code 0}).</p>
 */
public interface PurchaseRepository {

  Purchase save(Purchase purchase);

  Optional<Purchase> findByDownloadToken(UUID token);

  Optional<Purchase> findByStripePaymentIntentId(String stripePaymentIntentId);

  /**
   * Atomically transitions a PENDING purchase to PAID.
   *
   * @return {@code 1} if the row was updated (was PENDING), {@code 0} if it was
   *         already PAID/FAILED — i.e. the operation is idempotent.
   */
  int markPaid(String stripePaymentIntentId, Instant paidAt);

  /**
   * Atomically transitions a PENDING purchase to FAILED.
   *
   * @return {@code 1} if the row was updated, {@code 0} if it was already
   *         PAID/FAILED.
   */
  int markFailed(String stripePaymentIntentId);

  /** Returns PENDING purchases created before {@code cutoff} — used by the reconciliation scheduler. */
  List<Purchase> findPendingOlderThan(Instant cutoff);

  /**
   * Returns PAID purchases whose {@code emailSentAt} is still {@code NULL} and
   * whose {@code paidAt} is older than {@code graceCutoff}. Used by the email
   * retry function of the reconciliation scheduler.
   */
  List<Purchase> findPaidWithoutEmail(Instant graceCutoff);

  /** Persists the timestamp at which the download email was successfully delivered. */
  void updateEmailSentAt(UUID purchaseId, Instant emailSentAt);
}
