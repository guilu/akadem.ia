package com.akdemya.application.service;

import com.akdemya.adapter.infrastructure.stripe.StripeEventVerifierAdapter;
import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.akdemya.domain.port.in.DownloadPurchaseUseCase;
import com.akdemya.domain.port.in.GetPurchaseInfoUseCase;
import com.akdemya.domain.port.out.ProductCatalog;
import com.akdemya.domain.port.out.ProductFileStoragePort;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.net.LiveStripeResponseGetter;
import com.stripe.net.StripeResponseGetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

  private static final String PRODUCT_ID = "TEMARIO_SUBALTERNO_GVA";
  private static final String FRONTEND_URL = "https://akademia.test";
  private static final DigitalProduct PRODUCT = new DigitalProduct(
      PRODUCT_ID,
      "Temario Subalterno GVA",
      1500L,
      "eur",
      "temario.pdf",
      "Temario Subalterno GVA.pdf"
  );

  @Mock private PurchaseRepository purchaseRepository;
  @Mock private ProductCatalog productCatalog;
  @Mock private ProductFileStoragePort fileStorage;
  @Mock private TransactionalEmailPort emailPort;
  @Mock private StripePaymentGateway stripeGateway;
  @Mock private StripeEventVerifierAdapter eventVerifier;

  private PurchaseService service;

  @BeforeEach
  void setUp() {
    service = new PurchaseService(
        purchaseRepository,
        productCatalog,
        fileStorage,
        emailPort,
        stripeGateway,
        eventVerifier
    );
    ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
  }

  // -------------------------------------------------------------------------
  // createIntent
  // -------------------------------------------------------------------------

  @Test
  void createIntent_happyPath_persistsPendingPurchaseAndReturnsClientSecretAndDownloadToken() {
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
    when(stripeGateway.createPaymentIntent(any(), any(UUID.class), any(UUID.class)))
        .thenReturn(new StripePaymentGateway.GatewayResult("pi_secret_xyz", "pi_test_123"));

    CreatePaymentIntentUseCase.Result result = service.createIntent(
        new CreatePaymentIntentUseCase.Command("buyer@example.com", PRODUCT_ID, null));

    assertNotNull(result);
    assertEquals("pi_secret_xyz", result.clientSecret());
    assertNotNull(result.downloadToken());

    ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
    verify(purchaseRepository).save(captor.capture());
    Purchase saved = captor.getValue();
    assertEquals("buyer@example.com", saved.getEmail());
    assertEquals(PRODUCT_ID, saved.getProductId());
    assertEquals("pi_test_123", saved.getStripePaymentIntentId());
    assertEquals(PurchaseStatus.PENDING, saved.getStatus());
    assertEquals(1500L, saved.getAmountCents());
    assertEquals("eur", saved.getCurrency());
    assertEquals(result.downloadToken(), saved.getDownloadToken());
  }

  @Test
  void createIntent_unknownProductId_throwsAndDoesNotCallStripeOrRepository() {
    when(productCatalog.findById("UNKNOWN")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.createIntent(
        new CreatePaymentIntentUseCase.Command("buyer@example.com", "UNKNOWN", null)));

    verifyNoInteractions(stripeGateway);
    verify(purchaseRepository, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // handleEvent — payment_intent.succeeded
  // -------------------------------------------------------------------------

  @Test
  void handleEvent_paymentIntentSucceeded_markPaidReturns1_sendsEmailAndUpdatesEmailSentAt()
      throws com.stripe.exception.SignatureVerificationException {
    String piId = "pi_test_succeeded_1";
    UUID purchaseId = UUID.randomUUID();
    UUID downloadToken = UUID.randomUUID();
    Purchase purchase = paidPurchase(purchaseId, piId, downloadToken, "buyer@example.com");

    when(eventVerifier.constructEvent(anyString(), anyString()))
        .thenReturn(buildEvent("payment_intent.succeeded", piId));
    when(purchaseRepository.markPaid(eq(piId), any(Instant.class))).thenReturn(1);
    when(purchaseRepository.findByStripePaymentIntentId(piId)).thenReturn(Optional.of(purchase));
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
    when(emailPort.sendDownloadEmail(eq("buyer@example.com"), anyString(), eq("Temario Subalterno GVA")))
        .thenReturn(true);

    service.handleEvent("payload", "sig");

    verify(emailPort).sendDownloadEmail(eq("buyer@example.com"),
        eq(FRONTEND_URL + "/descarga/" + downloadToken), eq("Temario Subalterno GVA"));
    verify(purchaseRepository).updateEmailSentAt(eq(purchaseId), any(Instant.class));
  }

  @Test
  void handleEvent_paymentIntentSucceeded_markPaidReturns0_doesNotSendEmail()
      throws com.stripe.exception.SignatureVerificationException {
    String piId = "pi_test_idempotent";

    when(eventVerifier.constructEvent(anyString(), anyString()))
        .thenReturn(buildEvent("payment_intent.succeeded", piId));
    when(purchaseRepository.markPaid(eq(piId), any(Instant.class))).thenReturn(0);

    service.handleEvent("payload", "sig");

    verify(emailPort, never()).sendDownloadEmail(anyString(), anyString(), anyString());
    verify(purchaseRepository, never()).updateEmailSentAt(any(), any());
  }

  @Test
  void handleEvent_paymentIntentSucceeded_emailReturnsFalse_doesNotUpdateEmailSentAt()
      throws com.stripe.exception.SignatureVerificationException {
    String piId = "pi_test_email_fail";
    UUID purchaseId = UUID.randomUUID();
    UUID downloadToken = UUID.randomUUID();
    Purchase purchase = paidPurchase(purchaseId, piId, downloadToken, "buyer@example.com");

    when(eventVerifier.constructEvent(anyString(), anyString()))
        .thenReturn(buildEvent("payment_intent.succeeded", piId));
    when(purchaseRepository.markPaid(eq(piId), any(Instant.class))).thenReturn(1);
    when(purchaseRepository.findByStripePaymentIntentId(piId)).thenReturn(Optional.of(purchase));
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
    when(emailPort.sendDownloadEmail(anyString(), anyString(), anyString())).thenReturn(false);

    service.handleEvent("payload", "sig");

    verify(emailPort, times(1)).sendDownloadEmail(anyString(), anyString(), anyString());
    verify(purchaseRepository, never()).updateEmailSentAt(any(), any());
  }

  @Test
  void handleEvent_paymentIntentFailed_marksFailedAndDoesNotSendEmail()
      throws com.stripe.exception.SignatureVerificationException {
    String piId = "pi_test_failed";

    when(eventVerifier.constructEvent(anyString(), anyString()))
        .thenReturn(buildEvent("payment_intent.payment_failed", piId));

    service.handleEvent("payload", "sig");

    verify(purchaseRepository).markFailed(piId);
    verify(purchaseRepository, never()).markPaid(anyString(), any());
    verify(emailPort, never()).sendDownloadEmail(anyString(), anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // openByToken
  // -------------------------------------------------------------------------

  @Test
  void openByToken_paidPurchase_returnsProductAndStream() {
    UUID token = UUID.randomUUID();
    Purchase paid = paidPurchase(UUID.randomUUID(), "pi_x", token, "buyer@example.com");
    InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

    when(purchaseRepository.findByDownloadToken(token)).thenReturn(Optional.of(paid));
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
    when(fileStorage.open(PRODUCT.getStorageKey())).thenReturn(stream);

    DownloadPurchaseUseCase.Result result = service.openByToken(token);

    assertSame(PRODUCT, result.product());
    assertSame(stream, result.stream());
  }

  @Test
  void openByToken_pendingPurchase_throwsNoSuchElementException() {
    UUID token = UUID.randomUUID();
    Purchase pending = pendingPurchase(UUID.randomUUID(), "pi_x", token, "buyer@example.com");

    when(purchaseRepository.findByDownloadToken(token)).thenReturn(Optional.of(pending));

    assertThrows(NoSuchElementException.class, () -> service.openByToken(token));
    verifyNoInteractions(fileStorage);
  }

  @Test
  void openByToken_unknownToken_throwsNoSuchElementException() {
    UUID token = UUID.randomUUID();
    when(purchaseRepository.findByDownloadToken(token)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> service.openByToken(token));
  }

  // -------------------------------------------------------------------------
  // getInfo
  // -------------------------------------------------------------------------

  @Test
  void getInfo_paidPurchase_returnsBuyerSafeInfo() {
    UUID token = UUID.randomUUID();
    Purchase paid = paidPurchase(UUID.randomUUID(), "pi_x", token, "buyer@example.com");

    when(purchaseRepository.findByDownloadToken(token)).thenReturn(Optional.of(paid));
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));

    GetPurchaseInfoUseCase.PurchaseInfo info = service.getInfo(token);

    assertEquals("buyer@example.com", info.email());
    assertEquals("Temario Subalterno GVA", info.productName());
    assertEquals("PAID", info.status());
    assertEquals(1500L, info.amountCents());
    assertEquals("eur", info.currency());
  }

  @Test
  void getInfo_unknownToken_throwsNoSuchElementException() {
    UUID token = UUID.randomUUID();
    when(purchaseRepository.findByDownloadToken(token)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> service.getInfo(token));
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static Purchase paidPurchase(UUID id, String piId, UUID downloadToken, String email) {
    return new Purchase(
        id, piId, downloadToken, email, PRODUCT_ID, null, PurchaseStatus.PAID,
        1500L, "eur", Instant.now().minusSeconds(60), Instant.now(), null
    );
  }

  private static Purchase pendingPurchase(UUID id, String piId, UUID downloadToken, String email) {
    return new Purchase(
        id, piId, downloadToken, email, PRODUCT_ID, null, PurchaseStatus.PENDING,
        1500L, "eur", Instant.now(), null, null
    );
  }

  private static final StripeResponseGetter RESPONSE_GETTER = new LiveStripeResponseGetter();

  private static Event buildEvent(String type, String paymentIntentId) {
    String json = """
        {
          "id": "evt_test",
          "object": "event",
          "type": "%s",
          "api_version": "2024-04-10",
          "data": {
            "object": {
              "id": "%s",
              "object": "payment_intent",
              "status": "succeeded"
            }
          }
        }
        """.formatted(type, paymentIntentId);
    return StripeObject.deserializeStripeObject(json, Event.class, RESPONSE_GETTER);
  }
}
