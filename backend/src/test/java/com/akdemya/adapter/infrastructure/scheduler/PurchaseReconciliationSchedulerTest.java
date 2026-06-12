package com.akdemya.adapter.infrastructure.scheduler;

import com.akdemya.application.service.PurchaseService;
import com.akdemya.application.service.ReconciliationService;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.akdemya.domain.port.out.StripePaymentGateway.StripePaymentIntentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseReconciliationSchedulerTest {

  private static final String PRODUCT_ID = "TEMARIO_SUBALTERNO_GVA";

  @Mock private PurchaseRepository purchaseRepository;
  @Mock private StripePaymentGateway stripeGateway;
  @Mock private PurchaseService purchaseService;
  @Mock private ReconciliationService reconciliationService;

  private PurchaseReconciliationScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new PurchaseReconciliationScheduler(
        purchaseRepository, stripeGateway, purchaseService, reconciliationService);
  }

  // ---------------------------------------------------------------------------
  // Function A — reconcile PENDING with Stripe
  // ---------------------------------------------------------------------------

  @Test
  void reconcilePendingPurchases_succeededIntent_settlesPaidPurchase() {
    Purchase pending = pendingPurchase("pi_succeeded");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(pending));
    when(stripeGateway.retrieve("pi_succeeded"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_succeeded", "succeeded")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseService, times(1)).settlePaidPurchase("pi_succeeded");
    verify(purchaseRepository, never()).markFailed(any());
  }

  @Test
  void reconcilePendingPurchases_canceledIntent_marksFailed() {
    Purchase pending = pendingPurchase("pi_canceled");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(pending));
    when(stripeGateway.retrieve("pi_canceled"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_canceled", "canceled")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseRepository, times(1)).markFailed("pi_canceled");
    verify(purchaseService, never()).settlePaidPurchase(any());
  }

  @Test
  void reconcilePendingPurchases_abandonedIntent_cancelsAtStripeThenMarksFailed() {
    // requires_payment_method older than the 24h abandonment cutoff:
    // cancel at Stripe first, then mark FAILED.
    Purchase abandoned = pendingPurchase("pi_abandoned",
        Instant.now().minus(java.time.Duration.ofHours(25)));
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(abandoned));
    when(stripeGateway.retrieve("pi_abandoned"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_abandoned", "requires_payment_method")));

    scheduler.reconcilePendingPurchases();

    var order = org.mockito.Mockito.inOrder(stripeGateway, purchaseRepository);
    order.verify(stripeGateway).cancel("pi_abandoned");
    order.verify(purchaseRepository).markFailed("pi_abandoned");
    verify(purchaseService, never()).settlePaidPurchase(any());
  }

  @Test
  void reconcilePendingPurchases_recentRetryableIntent_staysPending() {
    // requires_payment_method but still within the abandonment window:
    // the customer may yet pay, so no cancel and no FAILED transition.
    Purchase recent = pendingPurchase("pi_recent");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(recent));
    when(stripeGateway.retrieve("pi_recent"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_recent", "requires_payment_method")));

    scheduler.reconcilePendingPurchases();

    verify(stripeGateway, never()).cancel(any());
    verify(purchaseRepository, never()).markFailed(any());
    verify(purchaseService, never()).settlePaidPurchase(any());
  }

  @Test
  void reconcilePendingPurchases_cancelFails_doesNotMarkFailed() {
    // If Stripe rejects the cancel (e.g. the intent just succeeded), the
    // purchase must NOT be marked FAILED — next tick re-reads the status.
    Purchase abandoned = pendingPurchase("pi_cancel_boom",
        Instant.now().minus(java.time.Duration.ofHours(25)));
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(abandoned));
    when(stripeGateway.retrieve("pi_cancel_boom"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_cancel_boom", "requires_payment_method")));
    org.mockito.Mockito.doThrow(new RuntimeException("already succeeded"))
        .when(stripeGateway).cancel("pi_cancel_boom");

    scheduler.reconcilePendingPurchases();

    verify(purchaseRepository, never()).markFailed(any());
  }

  @Test
  void reconcilePendingPurchases_processingIntent_skipsWithoutSideEffects() {
    Purchase pending = pendingPurchase("pi_processing");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(pending));
    when(stripeGateway.retrieve("pi_processing"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_processing", "processing")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseService, never()).settlePaidPurchase(any());
    verify(purchaseRepository, never()).markFailed(any());
  }

  @Test
  void reconcilePendingPurchases_requiresActionIntent_skipsWithoutSideEffects() {
    Purchase pending = pendingPurchase("pi_requires_action");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(pending));
    when(stripeGateway.retrieve("pi_requires_action"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_requires_action", "requires_action")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseService, never()).settlePaidPurchase(any());
    verify(purchaseRepository, never()).markFailed(any());
  }

  @Test
  void reconcilePendingPurchases_oneFailsOneSucceeds_continuesWithRest() {
    Purchase first = pendingPurchase("pi_boom");
    Purchase second = pendingPurchase("pi_succeeded");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(first, second));
    when(stripeGateway.retrieve("pi_boom"))
        .thenThrow(new RuntimeException("Stripe is angry"));
    when(stripeGateway.retrieve("pi_succeeded"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_succeeded", "succeeded")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseService, times(1)).settlePaidPurchase("pi_succeeded");
    verify(purchaseService, never()).settlePaidPurchase("pi_boom");
    verify(purchaseRepository, never()).markFailed(any());
  }

  @Test
  void reconcilePendingPurchases_findPendingUsesOneHourCutoff() {
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of());

    Instant before = Instant.now();
    scheduler.reconcilePendingPurchases();
    Instant after = Instant.now();

    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    verify(purchaseRepository).findPendingOlderThan(captor.capture());
    Instant cutoff = captor.getValue();

    Instant earliestExpected = before.minusSeconds(3600);
    Instant latestExpected = after.minusSeconds(3600);
    assertTrue(!cutoff.isBefore(earliestExpected) && !cutoff.isAfter(latestExpected),
        "cutoff must be ~now() - 1 hour, but was " + cutoff);
  }

  @Test
  void reconcilePendingPurchases_unknownStatus_skipsWithoutSideEffects() {
    Purchase pending = pendingPurchase("pi_weird");
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of(pending));
    when(stripeGateway.retrieve("pi_weird"))
        .thenReturn(Optional.of(new StripePaymentIntentSnapshot("pi_weird", "requires_payment_method")));

    scheduler.reconcilePendingPurchases();

    verify(purchaseService, never()).settlePaidPurchase(any());
    verify(purchaseRepository, never()).markFailed(any());
  }

  // ---------------------------------------------------------------------------
  // Function B — delegate email retries to ReconciliationService
  // ---------------------------------------------------------------------------

  @Test
  void retryFailedEmails_delegatesToReconciliationServiceWithFiveMinuteCutoff() {
    Instant before = Instant.now();
    scheduler.retryFailedEmails();
    Instant after = Instant.now();

    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    verify(reconciliationService).retryFailedEmails(captor.capture());
    Instant cutoff = captor.getValue();

    Instant earliestExpected = before.minusSeconds(300);
    Instant latestExpected = after.minusSeconds(300);
    assertTrue(!cutoff.isBefore(earliestExpected) && !cutoff.isAfter(latestExpected),
        "cutoff must be ~now() - 5 minutes, but was " + cutoff);
  }

  // ---------------------------------------------------------------------------
  // Single-tick orchestration: A runs before B
  // ---------------------------------------------------------------------------

  @Test
  void runReconciliationTick_invokesFunctionAThenFunctionB() {
    when(purchaseRepository.findPendingOlderThan(any(Instant.class)))
        .thenReturn(List.of());

    scheduler.runReconciliationTick();

    verify(purchaseRepository, times(1)).findPendingOlderThan(any(Instant.class));
    verify(reconciliationService, times(1)).retryFailedEmails(any(Instant.class));
    verifyNoInteractions(purchaseService);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static Purchase pendingPurchase(String piId) {
    return pendingPurchase(piId, Instant.now().minusSeconds(7200));
  }

  private static Purchase pendingPurchase(String piId, Instant createdAt) {
    return new Purchase(
        UUID.randomUUID(),
        piId,
        UUID.randomUUID(),
        "buyer@example.com",
        PRODUCT_ID,
        null,
        PurchaseStatus.PENDING,
        1500L,
        "eur",
        createdAt,
        null,
        null
    );
  }
}
