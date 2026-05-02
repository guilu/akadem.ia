package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.outbound.persistence.entity.PurchaseEntity;
import com.akdemya.adapter.outbound.persistence.repository.JpaPurchaseRepository;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the Stripe webhook endpoint
 * ({@code POST /api/v1/payments/webhook}).
 *
 * <p>Uses the real {@code StripeEventVerifierAdapter} and computes signature
 * headers locally with the test webhook secret defined in
 * {@code application-test.properties}. The transactional email port is mocked
 * so we can assert delivery side-effects without hitting Resend.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class WebhookControllerIT {

  private static final String WEBHOOK_SECRET = "whsec_test_secret_12345";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JpaPurchaseRepository purchaseRepository;

  @MockBean
  private TransactionalEmailPort transactionalEmailPort;

  @BeforeEach
  void setUp() {
    purchaseRepository.deleteAll();
    when(transactionalEmailPort.sendDownloadEmail(anyString(), anyString(), anyString()))
        .thenReturn(true);
  }

  @Test
  void webhook_validSignatureAndPaymentIntentSucceeded_returns200AndMarksPaidAndSendsEmail()
      throws Exception {
    String piId = "pi_test_succeeded_001";
    UUID downloadToken = UUID.randomUUID();
    seedPending(piId, downloadToken, "buyer@example.com");

    String payload = stripeEventJson("payment_intent.succeeded", piId, "succeeded");
    String signature = stripeSignature(payload, WEBHOOK_SECRET);

    mockMvc.perform(post("/api/v1/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", signature)
            .content(payload))
        .andExpect(status().isOk());

    Optional<PurchaseEntity> reloaded = purchaseRepository.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals("PAID", reloaded.get().getStatus());
    assertNotNull(reloaded.get().getPaidAt());
    assertNotNull(reloaded.get().getEmailSentAt(), "emailSentAt should be set after delivery");

    verify(transactionalEmailPort, times(1))
        .sendDownloadEmail(anyString(), anyString(), anyString());
  }

  @Test
  void webhook_idempotentRedelivery_marksPaidOnceAndSendsEmailOnce() throws Exception {
    String piId = "pi_test_idempotent_002";
    UUID downloadToken = UUID.randomUUID();
    seedPending(piId, downloadToken, "buyer@example.com");

    String payload = stripeEventJson("payment_intent.succeeded", piId, "succeeded");
    String signature = stripeSignature(payload, WEBHOOK_SECRET);

    // First delivery — settles the purchase and sends the email.
    mockMvc.perform(post("/api/v1/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", signature)
            .content(payload))
        .andExpect(status().isOk());

    // Re-delivery of the same event must be a no-op for the email port.
    String secondSignature = stripeSignature(payload, WEBHOOK_SECRET);
    mockMvc.perform(post("/api/v1/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", secondSignature)
            .content(payload))
        .andExpect(status().isOk());

    verify(transactionalEmailPort, times(1))
        .sendDownloadEmail(anyString(), anyString(), anyString());

    Optional<PurchaseEntity> reloaded = purchaseRepository.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals("PAID", reloaded.get().getStatus());
  }

  @Test
  void webhook_invalidSignature_returns400AndDoesNotMutateState() throws Exception {
    String piId = "pi_test_badsig_003";
    UUID downloadToken = UUID.randomUUID();
    seedPending(piId, downloadToken, "buyer@example.com");

    String payload = stripeEventJson("payment_intent.succeeded", piId, "succeeded");
    // Compute signature with a different secret — verification must reject it.
    String wrongSignature = stripeSignature(payload, "whsec_wrong_secret");

    mockMvc.perform(post("/api/v1/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", wrongSignature)
            .content(payload))
        .andExpect(status().isBadRequest());

    Optional<PurchaseEntity> reloaded = purchaseRepository.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals("PENDING", reloaded.get().getStatus());
    assertNull(reloaded.get().getPaidAt());

    verifyNoInteractions(transactionalEmailPort);
  }

  @Test
  void webhook_paymentIntentFailed_returns200AndMarksFailedAndDoesNotSendEmail()
      throws Exception {
    String piId = "pi_test_failed_004";
    UUID downloadToken = UUID.randomUUID();
    seedPending(piId, downloadToken, "buyer@example.com");

    String payload = stripeEventJson("payment_intent.payment_failed", piId, "requires_payment_method");
    String signature = stripeSignature(payload, WEBHOOK_SECRET);

    mockMvc.perform(post("/api/v1/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", signature)
            .content(payload))
        .andExpect(status().isOk());

    Optional<PurchaseEntity> reloaded = purchaseRepository.findByStripePaymentIntentId(piId);
    assertTrue(reloaded.isPresent());
    assertEquals("FAILED", reloaded.get().getStatus());
    assertNull(reloaded.get().getPaidAt());

    verifyNoInteractions(transactionalEmailPort);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // helpers
  // ─────────────────────────────────────────────────────────────────────────

  private void seedPending(String piId, UUID downloadToken, String email) {
    PurchaseEntity e = new PurchaseEntity();
    e.setId(UUID.randomUUID());
    e.setStripePaymentIntentId(piId);
    e.setDownloadToken(downloadToken);
    e.setEmail(email);
    e.setProductId("TEMARIO_SUBALTERNO_GVA");
    e.setStatus("PENDING");
    e.setAmountCents(1500L);
    e.setCurrency("eur");
    e.setCreatedAt(Instant.now());
    purchaseRepository.save(e);
  }

  private static String stripeEventJson(String type, String piId, String piStatus) {
    return """
        {
          "id": "evt_%s",
          "object": "event",
          "type": "%s",
          "api_version": "2024-04-10",
          "data": {
            "object": {
              "id": "%s",
              "object": "payment_intent",
              "status": "%s"
            }
          }
        }
        """.formatted(piId, type, piId, piStatus);
  }

  /** Builds a Stripe-Signature header value for the given payload + secret. */
  private static String stripeSignature(String payload, String secret) {
    long timestamp = Instant.now().getEpochSecond();
    String signedPayload = timestamp + "." + payload;
    String hex = hmacSha256Hex(signedPayload, secret);
    return "t=" + timestamp + ",v1=" + hex;
  }

  private static String hmacSha256Hex(String data, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(raw.length * 2);
      for (byte b : raw) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("HMAC computation failed", ex);
    }
  }
}
