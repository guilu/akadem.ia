package com.akdemya.application.service;

import com.akdemya.adapter.infrastructure.stripe.StripeEventVerifierAdapter;
import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.akdemya.domain.port.in.DownloadPurchaseUseCase;
import com.akdemya.domain.port.in.GetPurchaseInfoUseCase;
import com.akdemya.domain.port.in.HandleStripeWebhookUseCase;
import com.akdemya.domain.port.out.ProductCatalog;
import com.akdemya.domain.port.out.ProductFileStoragePort;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service that orchestrates the full digital-product purchase
 * lifecycle: PaymentIntent creation, Stripe webhook handling, file download
 * resolution and buyer-safe purchase info lookup.
 *
 * <p>Implements the four inbound use cases — {@link CreatePaymentIntentUseCase},
 * {@link HandleStripeWebhookUseCase}, {@link DownloadPurchaseUseCase} and
 * {@link GetPurchaseInfoUseCase} — and delegates side effects to outbound
 * ports so the domain core stays free of Stripe / persistence / email
 * concerns.</p>
 */
@Service
@Transactional
public class PurchaseService implements
    CreatePaymentIntentUseCase,
    HandleStripeWebhookUseCase,
    DownloadPurchaseUseCase,
    GetPurchaseInfoUseCase {

  private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

  private final PurchaseRepository purchaseRepository;
  private final ProductCatalog productCatalog;
  private final ProductFileStoragePort fileStorage;
  private final TransactionalEmailPort emailPort;
  private final StripePaymentGateway stripeGateway;
  private final StripeEventVerifierAdapter eventVerifier;

  @Value("${app.frontend-url}")
  private String frontendUrl;

  public PurchaseService(PurchaseRepository purchaseRepository,
                         ProductCatalog productCatalog,
                         ProductFileStoragePort fileStorage,
                         TransactionalEmailPort emailPort,
                         StripePaymentGateway stripeGateway,
                         StripeEventVerifierAdapter eventVerifier) {
    this.purchaseRepository = purchaseRepository;
    this.productCatalog = productCatalog;
    this.fileStorage = fileStorage;
    this.emailPort = emailPort;
    this.stripeGateway = stripeGateway;
    this.eventVerifier = eventVerifier;
  }

  // ---------------------------------------------------------------------------
  // CreatePaymentIntentUseCase
  // ---------------------------------------------------------------------------

  @Override
  public CreatePaymentIntentUseCase.Result createIntent(Command command) {
    if (command == null) {
      throw new IllegalArgumentException("command cannot be null");
    }

    DigitalProduct product = productCatalog.findById(command.productId())
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown productId: " + command.productId()));

    UUID purchaseId = UUID.randomUUID();
    UUID downloadToken = UUID.randomUUID();

    StripePaymentGateway.GatewayResult gatewayResult =
        stripeGateway.createPaymentIntent(command, purchaseId, downloadToken);

    Purchase purchase = new Purchase(
        purchaseId,
        gatewayResult.paymentIntentId(),
        downloadToken,
        command.email(),
        product.getSku(),
        command.userId(),
        PurchaseStatus.PENDING,
        product.getAmountCents(),
        product.getCurrency(),
        Instant.now(),
        null,
        null
    );
    purchaseRepository.save(purchase);

    log.info("Created PaymentIntent {} for purchaseId={} productId={}",
        gatewayResult.paymentIntentId(), purchaseId, product.getSku());

    return new CreatePaymentIntentUseCase.Result(gatewayResult.clientSecret(), downloadToken);
  }

  // ---------------------------------------------------------------------------
  // HandleStripeWebhookUseCase
  // ---------------------------------------------------------------------------

  @Override
  public void handleEvent(String rawPayload, String stripeSignatureHeader) {
    Event event;
    try {
      event = eventVerifier.constructEvent(rawPayload, stripeSignatureHeader);
    } catch (SignatureVerificationException ex) {
      log.warn("Stripe webhook signature verification failed: {}", ex.getMessage());
      throw new IllegalArgumentException("Invalid Stripe signature", ex);
    }

    String type = event.getType();
    switch (type) {
      case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
      case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
      default -> log.info("Ignoring unhandled Stripe event type: {}", type);
    }
  }

  private void handlePaymentIntentSucceeded(Event event) {
    String piId = extractPaymentIntentId(event);
    if (piId == null) {
      log.warn("payment_intent.succeeded event without a PaymentIntent id; ignoring");
      return;
    }
    settlePaidPurchase(piId);
  }

  /**
   * Atomically marks the {@link Purchase} associated with the given Stripe
   * PaymentIntent as PAID and, on a successful state transition, sends the
   * download email and persists the delivery timestamp.
   *
   * <p>Shared between the webhook handler and the
   * {@code PurchaseReconciliationScheduler} (Function A) so both paths agree
   * on idempotency and email-retry semantics.</p>
   *
   * <p>Idempotent: when {@code markPaid} returns {@code 0} the row is already
   * PAID/FAILED and no email is sent.</p>
   */
  public void settlePaidPurchase(String piId) {
    if (piId == null || piId.isBlank()) {
      log.warn("settlePaidPurchase called with blank PaymentIntent id; ignoring");
      return;
    }

    Instant now = Instant.now();
    int rows = purchaseRepository.markPaid(piId, now);
    if (rows == 0) {
      log.info("Idempotent settlement for paymentIntent={} — already settled", piId);
      return;
    }

    Optional<Purchase> maybePurchase = purchaseRepository.findByStripePaymentIntentId(piId);
    if (maybePurchase.isEmpty()) {
      log.warn("Marked paid but Purchase not found for paymentIntent={}", piId);
      return;
    }
    Purchase purchase = maybePurchase.get();

    Optional<DigitalProduct> maybeProduct = productCatalog.findById(purchase.getProductId());
    if (maybeProduct.isEmpty()) {
      log.warn("Marked paid but product not found for productId={} purchaseId={}",
          purchase.getProductId(), purchase.getId());
      return;
    }
    DigitalProduct product = maybeProduct.get();

    // Send the email only after the surrounding transaction commits. Sending
    // inside it risks (a) a duplicate email if the transaction rolls back
    // after the send — markPaid is undone, so the scheduler re-settles and
    // re-sends — and (b) holding a DB connection during an external HTTP call.
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          deliverDownloadEmail(purchase, product);
        }
      });
    } else {
      deliverDownloadEmail(purchase, product);
    }
  }

  /**
   * Sends the download email and records {@code emailSentAt} on success.
   * Runs outside the settlement transaction (after commit); the
   * {@code updateEmailSentAt} adapter opens its own transaction. On failure
   * the purchase stays PAID without {@code emailSentAt}, so the
   * reconciliation scheduler retries delivery.
   */
  private void deliverDownloadEmail(Purchase purchase, DigitalProduct product) {
    String downloadUrl = frontendUrl + "/descarga/" + purchase.getDownloadToken();
    boolean delivered = emailPort.sendDownloadEmail(
        purchase.getEmail(), downloadUrl, product.getDisplayName());
    if (delivered) {
      purchaseRepository.updateEmailSentAt(purchase.getId(), Instant.now());
      log.info("Download email sent for purchaseId={}", purchase.getId());
    } else {
      log.error("Resend delivery failed for purchaseId={} — scheduler will retry",
          purchase.getId());
    }
  }

  private void handlePaymentIntentFailed(Event event) {
    String piId = extractPaymentIntentId(event);
    if (piId == null) {
      log.warn("payment_intent.payment_failed event without a PaymentIntent id; ignoring");
      return;
    }
    int rows = purchaseRepository.markFailed(piId);
    if (rows == 0) {
      log.info("Idempotent failure delivery for paymentIntent={} — already settled", piId);
    } else {
      log.info("Marked Purchase as FAILED for paymentIntent={}", piId);
    }
  }

  private String extractPaymentIntentId(Event event) {
    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
    Optional<StripeObject> deserialized = deserializer.getObject();
    StripeObject obj = null;
    if (deserialized.isPresent()) {
      obj = deserialized.get();
    } else {
      // Falls through if the event's apiVersion differs from the SDK's bound
      // api version — common in Stripe Dashboard test events. Falling back to
      // the unsafe deserializer parses the raw JSON regardless of versions.
      try {
        obj = deserializer.deserializeUnsafe();
      } catch (EventDataObjectDeserializationException ex) {
        log.warn("Failed to deserialize Stripe event data object: {}", ex.getMessage());
        return null;
      }
    }
    if (obj instanceof PaymentIntent pi) {
      return pi.getId();
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // DownloadPurchaseUseCase
  // ---------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public DownloadPurchaseUseCase.Result openByToken(UUID downloadToken) {
    Purchase purchase = purchaseRepository.findByDownloadToken(downloadToken)
        .orElseThrow(() -> new NoSuchElementException(
            "No purchase found for downloadToken=" + downloadToken));

    if (purchase.getStatus() != PurchaseStatus.PAID) {
      throw new NoSuchElementException(
          "Purchase is not PAID for downloadToken=" + downloadToken);
    }

    DigitalProduct product = productCatalog.findById(purchase.getProductId())
        .orElseThrow(() -> new NoSuchElementException(
            "Unknown productId for purchase: " + purchase.getProductId()));

    InputStream stream = fileStorage.open(product.getStorageKey());
    return new DownloadPurchaseUseCase.Result(product, stream);
  }

  // ---------------------------------------------------------------------------
  // GetPurchaseInfoUseCase
  // ---------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public PurchaseInfo getInfo(UUID downloadToken) {
    Purchase purchase = purchaseRepository.findByDownloadToken(downloadToken)
        .orElseThrow(() -> new NoSuchElementException(
            "No purchase found for downloadToken=" + downloadToken));

    DigitalProduct product = productCatalog.findById(purchase.getProductId())
        .orElseThrow(() -> new NoSuchElementException(
            "Unknown productId for purchase: " + purchase.getProductId()));

    return new PurchaseInfo(
        purchase.getEmail(),
        product.getDisplayName(),
        purchase.getStatus().name(),
        purchase.getAmountCents(),
        purchase.getCurrency()
    );
  }
}
