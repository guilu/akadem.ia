package com.akdemya.application.service;

import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.port.out.ProductCatalog;
import com.akdemya.domain.port.out.PurchaseRepository;
import com.akdemya.domain.port.out.TransactionalEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service for the asynchronous reconciliation of purchases whose
 * download email failed to deliver at webhook time.
 *
 * <p>Function B from {@code design.md §6}: scans for {@code PAID} purchases that
 * have no {@code emailSentAt} timestamp and re-attempts delivery through
 * {@link TransactionalEmailPort}. The grace cutoff prevents racing with the
 * webhook handler — only purchases paid before the cutoff are retried.</p>
 */
@Service
public class ReconciliationService {

  private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

  private final PurchaseRepository purchaseRepository;
  private final ProductCatalog productCatalog;
  private final TransactionalEmailPort emailPort;

  @Value("${app.frontend-url}")
  private String frontendUrl;

  public ReconciliationService(PurchaseRepository purchaseRepository,
                               ProductCatalog productCatalog,
                               TransactionalEmailPort emailPort) {
    this.purchaseRepository = purchaseRepository;
    this.productCatalog = productCatalog;
    this.emailPort = emailPort;
  }

  /**
   * Re-attempts download-email delivery for every {@code PAID} purchase whose
   * email has not been recorded as sent and whose {@code paidAt} is older than
   * the supplied {@code graceCutoff}. Failures are logged at WARN and left for
   * the next scheduler tick — no exception is propagated for individual rows.
   */
  public void retryFailedEmails(Instant graceCutoff) {
    List<Purchase> pending = purchaseRepository.findPaidWithoutEmail(graceCutoff);
    log.info("Reconciliation: retrying download email for {} purchase(s)", pending.size());

    for (Purchase purchase : pending) {
      Optional<DigitalProduct> maybeProduct = productCatalog.findById(purchase.getProductId());
      if (maybeProduct.isEmpty()) {
        log.warn("Reconciliation: product not found for productId={} purchaseId={}",
            purchase.getProductId(), purchase.getId());
        continue;
      }
      DigitalProduct product = maybeProduct.get();

      String downloadUrl = frontendUrl + "/descarga/" + purchase.getDownloadToken();
      try {
        boolean delivered = emailPort.sendDownloadEmail(
            purchase.getEmail(), downloadUrl, product.getDisplayName());
        if (delivered) {
          purchaseRepository.updateEmailSentAt(purchase.getId(), Instant.now());
          log.info("Reconciliation: download email sent for purchaseId={}", purchase.getId());
        } else {
          log.warn("Reconciliation: email delivery failed for purchaseId={} — will retry next tick",
              purchase.getId());
        }
      } catch (RuntimeException ex) {
        log.warn("Reconciliation: unexpected error retrying email for purchaseId={}: {}",
            purchase.getId(), ex.getMessage());
      }
    }
  }
}
