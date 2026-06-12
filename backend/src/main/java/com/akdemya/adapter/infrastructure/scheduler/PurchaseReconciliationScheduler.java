package com.akdemya.adapter.infrastructure.scheduler;

import com.akdemya.application.service.PurchaseService;
import com.akdemya.application.service.ReconciliationService;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.akdemya.domain.port.out.StripePaymentGateway.StripePaymentIntentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled reconciliation for the digital-product purchase flow.
 *
 * <p>Runs every 15 minutes (cron {@code "0 *&#47;15 * * * *"}, matching the
 * style of {@link RefreshTokenCleanupJob}) and performs two passes per tick:
 * </p>
 *
 * <ol>
 *   <li><b>Function A — PENDING reconciliation</b>: scans purchases that have
 *       been PENDING for more than one hour and asks Stripe for the latest
 *       PaymentIntent status. {@code succeeded} → delegate to
 *       {@link PurchaseService#settlePaidPurchase(String)} (idempotent: shares
 *       the same {@code markPaid + email + updateEmailSentAt} flow used by the
 *       webhook). {@code canceled} → {@link PurchaseRepository#markFailed(String)}.
 *       {@code requires_payment_method}/{@code requires_confirmation}/
 *       {@code requires_action} older than 24h → cancel at Stripe, then mark
 *       FAILED. Other statuses ({@code processing}, recent retryable ones) are
 *       left PENDING and re-checked next tick.</li>
 *   <li><b>Function B — email retries</b>: delegates to
 *       {@link ReconciliationService#retryFailedEmails(Instant)} with a
 *       5-minute grace cutoff to avoid racing with the webhook handler.</li>
 * </ol>
 *
 * <p>Per-purchase exceptions in Function A are caught and logged; one failing
 * purchase MUST NOT abort the batch. See {@code design.md §7}.</p>
 */
@Component
public class PurchaseReconciliationScheduler {

  private static final Logger log = LoggerFactory.getLogger(PurchaseReconciliationScheduler.class);

  private static final Duration PENDING_AGE_CUTOFF = Duration.ofHours(1);
  private static final Duration EMAIL_RETRY_GRACE = Duration.ofMinutes(5);

  /**
   * Age after which a still-unpaid PaymentIntent is considered abandoned and
   * is canceled at Stripe + marked FAILED. A failed card attempt sends the
   * intent back to {@code requires_payment_method} (there is no
   * {@code payment_failed} status), so without this cutoff abandoned
   * purchases would stay PENDING and be re-polled forever.
   */
  private static final Duration ABANDONED_CUTOFF = Duration.ofHours(24);

  private final PurchaseRepository purchaseRepository;
  private final StripePaymentGateway stripeGateway;
  private final PurchaseService purchaseService;
  private final ReconciliationService reconciliationService;

  public PurchaseReconciliationScheduler(PurchaseRepository purchaseRepository,
                                         StripePaymentGateway stripeGateway,
                                         PurchaseService purchaseService,
                                         ReconciliationService reconciliationService) {
    this.purchaseRepository = purchaseRepository;
    this.stripeGateway = stripeGateway;
    this.purchaseService = purchaseService;
    this.reconciliationService = reconciliationService;
  }

  /**
   * Spring entry point — runs every 15 minutes on the minute. Executes
   * Function A, then Function B, in that order. Wrapping both in a single
   * scheduled method keeps the work cohesive and lets us log a single tick.
   */
  @Scheduled(cron = "0 */15 * * * *")
  public void runReconciliationTick() {
    log.info("Reconciliation tick started");
    reconcilePendingPurchases();
    retryFailedEmails();
    log.info("Reconciliation tick finished");
  }

  /**
   * Function A — reconciles {@code PENDING} purchases against Stripe so the
   * application catches up when a webhook delivery was missed.
   */
  void reconcilePendingPurchases() {
    Instant cutoff = Instant.now().minus(PENDING_AGE_CUTOFF);
    List<Purchase> pending = purchaseRepository.findPendingOlderThan(cutoff);
    log.info("Reconciliation: checking {} PENDING purchase(s) older than {}",
        pending.size(), cutoff);

    for (Purchase purchase : pending) {
      String piId = purchase.getStripePaymentIntentId();
      try {
        Optional<StripePaymentIntentSnapshot> maybeSnapshot = stripeGateway.retrieve(piId);
        if (maybeSnapshot.isEmpty()) {
          log.warn("Reconciliation: Stripe returned no PaymentIntent for piId={} purchaseId={}",
              piId, purchase.getId());
          continue;
        }
        String status = maybeSnapshot.get().status();
        switch (status) {
          case "succeeded" -> purchaseService.settlePaidPurchase(piId);
          case "canceled" -> markFailed(piId);
          // A failed or never-attempted payment leaves the intent in one of
          // these states ("payment_failed" is an event type, not a status).
          // The customer may still complete payment from an open tab, so only
          // give up after ABANDONED_CUTOFF — and cancel at Stripe FIRST, so
          // the intent can no longer succeed once we mark the purchase FAILED.
          case "requires_payment_method", "requires_confirmation", "requires_action" -> {
            if (purchase.getCreatedAt().isBefore(Instant.now().minus(ABANDONED_CUTOFF))) {
              stripeGateway.cancel(piId);
              markFailed(piId);
            } else {
              log.info("Reconciliation: piId={} status={} — within abandonment window, leaving PENDING",
                  piId, status);
            }
          }
          // "processing" and any future statuses: leave PENDING, re-check next tick.
          default -> log.info("Reconciliation: skipping piId={} — status={} not final",
              piId, status);
        }
      } catch (RuntimeException ex) {
        log.error("Reconciliation: error processing piId={} purchaseId={}: {}",
            piId, purchase.getId(), ex.getMessage(), ex);
      }
    }
  }

  private void markFailed(String piId) {
    int rows = purchaseRepository.markFailed(piId);
    if (rows == 1) {
      log.info("Reconciliation: marked Purchase as FAILED for paymentIntent={}", piId);
    } else {
      log.info("Reconciliation: idempotent FAILED transition for paymentIntent={}", piId);
    }
  }

  /**
   * Function B — delegates email retries for {@code PAID} purchases without an
   * {@code emailSentAt} timestamp to {@link ReconciliationService}. Uses a
   * 5-minute grace cutoff so the webhook handler has time to commit
   * {@code updateEmailSentAt} before the scheduler considers the purchase
   * stale.
   */
  void retryFailedEmails() {
    Instant graceCutoff = Instant.now().minus(EMAIL_RETRY_GRACE);
    log.info("Reconciliation: retrying download emails with graceCutoff={}", graceCutoff);
    reconciliationService.retryFailedEmails(graceCutoff);
  }
}
