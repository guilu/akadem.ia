package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.outbound.persistence.entity.PurchaseEntity;
import com.akdemya.adapter.outbound.persistence.repository.JpaPurchaseRepository;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class PaymentControllerIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JpaPurchaseRepository purchaseRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private StripePaymentGateway stripePaymentGateway;

  @BeforeEach
  void setUp() {
    purchaseRepository.deleteAll();
  }

  @Test
  void createIntent_validBody_returns200WithClientSecretAndDownloadTokenAndPersistsPendingPurchase()
      throws Exception {
    when(stripePaymentGateway.createPaymentIntent(any(CreatePaymentIntentUseCase.Command.class),
        any(UUID.class), any(UUID.class)))
        .thenReturn(new StripePaymentGateway.GatewayResult("pi_test_secret", "pi_123"));

    String body = """
        { "email": "buyer@example.com", "productId": "TEMARIO_SUBALTERNO_GVA" }
        """;

    MvcResult mvcResult = mockMvc.perform(post("/api/v1/payments/create-intent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andReturn();

    Map<String, Object> response = objectMapper.readValue(
        mvcResult.getResponse().getContentAsString(),
        new com.fasterxml.jackson.core.type.TypeReference<>() {});

    assertEquals("pi_test_secret", response.get("clientSecret"));
    assertNotNull(response.get("downloadToken"));
    UUID downloadToken = UUID.fromString((String) response.get("downloadToken"));

    List<PurchaseEntity> rows = purchaseRepository.findAll();
    assertEquals(1, rows.size(), "exactly one Purchase row should be persisted");
    PurchaseEntity saved = rows.get(0);
    assertEquals("buyer@example.com", saved.getEmail());
    assertEquals("TEMARIO_SUBALTERNO_GVA", saved.getProductId());
    assertEquals("pi_123", saved.getStripePaymentIntentId());
    assertEquals(downloadToken, saved.getDownloadToken());
    assertEquals("PENDING", saved.getStatus());
    assertEquals(1500L, saved.getAmountCents());
    assertEquals("eur", saved.getCurrency());
  }

  @Test
  void createIntent_blankEmail_returns400AndDoesNotCallGateway() throws Exception {
    String body = """
        { "email": "", "productId": "TEMARIO_SUBALTERNO_GVA" }
        """;

    mockMvc.perform(post("/api/v1/payments/create-intent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stripePaymentGateway);
    assertTrue(purchaseRepository.findAll().isEmpty(),
        "no Purchase row should be persisted on validation failure");
  }

  @Test
  void createIntent_missingEmailField_returns400AndDoesNotCallGateway() throws Exception {
    String body = """
        { "productId": "TEMARIO_SUBALTERNO_GVA" }
        """;

    mockMvc.perform(post("/api/v1/payments/create-intent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stripePaymentGateway);
    assertTrue(purchaseRepository.findAll().isEmpty());
  }

  @Test
  void createIntent_unknownProductId_returns400AndDoesNotCallGateway() throws Exception {
    String body = """
        { "email": "buyer@example.com", "productId": "UNKNOWN_SKU" }
        """;

    mockMvc.perform(post("/api/v1/payments/create-intent")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stripePaymentGateway);
    assertTrue(purchaseRepository.findAll().isEmpty(),
        "no Purchase row should be persisted when productId is unknown");
  }
}
