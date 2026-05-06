package com.akdemya.application.service;

import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import com.akdemya.domain.port.out.ProductCatalog;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

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
  @Mock private TransactionalEmailPort emailPort;

  private ReconciliationService service;

  @BeforeEach
  void setUp() {
    service = new ReconciliationService(purchaseRepository, productCatalog, emailPort);
    ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
  }

  @Test
  void retryFailedEmails_emailsTwoPurchases_updatesOnlyTheSuccessfulOne() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Purchase p1 = paidPurchase(id1, "pi_1", UUID.randomUUID(), "buyer1@example.com");
    Purchase p2 = paidPurchase(id2, "pi_2", UUID.randomUUID(), "buyer2@example.com");
    Instant cutoff = Instant.now().minusSeconds(300);

    when(purchaseRepository.findPaidWithoutEmail(cutoff)).thenReturn(List.of(p1, p2));
    when(productCatalog.findById(PRODUCT_ID)).thenReturn(Optional.of(PRODUCT));
    when(emailPort.sendDownloadEmail(eq("buyer1@example.com"), anyString(), eq("Temario Subalterno GVA")))
        .thenReturn(true);
    when(emailPort.sendDownloadEmail(eq("buyer2@example.com"), anyString(), eq("Temario Subalterno GVA")))
        .thenReturn(false);

    service.retryFailedEmails(cutoff);

    verify(emailPort, times(1)).sendDownloadEmail(eq("buyer1@example.com"), anyString(), anyString());
    verify(emailPort, times(1)).sendDownloadEmail(eq("buyer2@example.com"), anyString(), anyString());
    verify(purchaseRepository).updateEmailSentAt(eq(id1), any(Instant.class));
    verify(purchaseRepository, never()).updateEmailSentAt(eq(id2), any());
  }

  @Test
  void retryFailedEmails_noPendingPurchases_doesNothing() {
    Instant cutoff = Instant.now().minusSeconds(300);
    when(purchaseRepository.findPaidWithoutEmail(cutoff)).thenReturn(List.of());

    service.retryFailedEmails(cutoff);

    verify(emailPort, never()).sendDownloadEmail(anyString(), anyString(), anyString());
    verify(purchaseRepository, never()).updateEmailSentAt(any(), any());
  }

  private static Purchase paidPurchase(UUID id, String piId, UUID downloadToken, String email) {
    return new Purchase(
        id, piId, downloadToken, email, PRODUCT_ID, null, PurchaseStatus.PAID,
        1500L, "eur", Instant.now().minusSeconds(600), Instant.now().minusSeconds(400), null
    );
  }
}
