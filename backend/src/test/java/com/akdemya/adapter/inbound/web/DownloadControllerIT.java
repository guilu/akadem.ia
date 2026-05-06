package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.outbound.persistence.entity.PurchaseEntity;
import com.akdemya.adapter.outbound.persistence.repository.JpaPurchaseRepository;
import com.akdemya.domain.port.out.ProductFileStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@link DownloadController}.
 *
 * <p>The {@link ProductFileStoragePort} is mocked so the test does not depend on
 * a PDF being on disk; the catalog is the production {@code InMemoryProductCatalog}
 * which knows {@code TEMARIO_SUBALTERNO_GVA → temario-subalterno-gva.pdf}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class DownloadControllerIT {

  private static final byte[] PDF_BYTES = "PDFBYTES_TEST_PAYLOAD".getBytes();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JpaPurchaseRepository purchaseRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProductFileStoragePort productFileStoragePort;

  @BeforeEach
  void setUp() {
    purchaseRepository.deleteAll();
    when(productFileStoragePort.open(eq("temario-subalterno-gva.pdf")))
        .thenReturn(new ByteArrayInputStream(PDF_BYTES));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/v1/downloads/{token}
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void download_paidPurchase_returns200WithPdfStreamAndAttachmentHeader() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_paid_001", token, "buyer@example.com", "PAID", Instant.now());

    MvcResult result = mockMvc.perform(get("/api/v1/downloads/" + token))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"))
        .andExpect(header().string("Content-Disposition",
            "attachment; filename=\"Temario Subalterno GVA.pdf\""))
        .andReturn();

    // For StreamingResponseBody we need to drive the async dispatch to completion.
    byte[] body = mockMvc.perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsByteArray();

    assertArrayEquals(PDF_BYTES, body);
  }

  @Test
  void download_pendingPurchase_returns404() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_pending_002", token, "buyer@example.com", "PENDING", null);

    mockMvc.perform(get("/api/v1/downloads/" + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void download_failedPurchase_returns404() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_failed_003", token, "buyer@example.com", "FAILED", null);

    mockMvc.perform(get("/api/v1/downloads/" + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void download_unknownToken_returns404() throws Exception {
    mockMvc.perform(get("/api/v1/downloads/" + UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void download_nonUuidToken_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/downloads/not-a-uuid"))
        .andExpect(status().isBadRequest());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/v1/downloads/{token}/info
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void info_paidPurchase_returns200WithPurchaseInfoBody() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_info_paid", token, "buyer@example.com", "PAID", Instant.now());

    MvcResult mvcResult = mockMvc.perform(get("/api/v1/downloads/" + token + "/info"))
        .andExpect(status().isOk())
        .andReturn();

    Map<String, Object> body = objectMapper.readValue(
        mvcResult.getResponse().getContentAsString(),
        new com.fasterxml.jackson.core.type.TypeReference<>() {});

    assertEquals("buyer@example.com", body.get("email"));
    assertEquals("Temario Subalterno GVA", body.get("productName"));
    assertEquals("PAID", body.get("status"));
    assertEquals(1500, ((Number) body.get("amountCents")).intValue());
    assertEquals("eur", body.get("currency"));
  }

  @Test
  void info_pendingPurchase_returns200WithPendingStatus() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_info_pending", token, "buyer@example.com", "PENDING", null);

    mockMvc.perform(get("/api/v1/downloads/" + token + "/info"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"PENDING\"")));
  }

  @Test
  void info_failedPurchase_returns200WithFailedStatus() throws Exception {
    UUID token = UUID.randomUUID();
    seedPurchase("pi_info_failed", token, "buyer@example.com", "FAILED", null);

    mockMvc.perform(get("/api/v1/downloads/" + token + "/info"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"FAILED\"")));
  }

  @Test
  void info_unknownToken_returns404() throws Exception {
    mockMvc.perform(get("/api/v1/downloads/" + UUID.randomUUID() + "/info"))
        .andExpect(status().isNotFound());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // helpers
  // ─────────────────────────────────────────────────────────────────────────

  private void seedPurchase(String piId, UUID token, String email, String status, Instant paidAt) {
    PurchaseEntity e = new PurchaseEntity();
    e.setId(UUID.randomUUID());
    e.setStripePaymentIntentId(piId);
    e.setDownloadToken(token);
    e.setEmail(email);
    e.setProductId("TEMARIO_SUBALTERNO_GVA");
    e.setStatus(status);
    e.setAmountCents(1500L);
    e.setCurrency("eur");
    e.setCreatedAt(Instant.now().minusSeconds(60));
    e.setPaidAt(paidAt);
    purchaseRepository.save(e);
  }

  private static org.springframework.test.web.servlet.RequestBuilder asyncDispatch(MvcResult result) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .asyncDispatch(result);
  }
}
