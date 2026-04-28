package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.PurchaseMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaPurchaseRepository;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.port.out.PurchaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter for the {@link PurchaseRepository} port.
 *
 * <p>Modifying queries ({@code markPaid}, {@code markFailed}, {@code updateEmailSentAt})
 * are delegated to {@link JpaPurchaseRepository} where they live as
 * {@code @Modifying @Query} annotations. {@code markPaid} / {@code markFailed} return
 * the number of affected rows, allowing callers to detect idempotent retries.</p>
 */
@Component
public class PurchaseRepositoryAdapter implements PurchaseRepository {

  private final JpaPurchaseRepository jpa;
  private final PurchaseMapper mapper;

  public PurchaseRepositoryAdapter(JpaPurchaseRepository jpa, PurchaseMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public Purchase save(Purchase purchase) {
    return mapper.toDomain(jpa.save(mapper.toEntity(purchase)));
  }

  @Override
  public Optional<Purchase> findByDownloadToken(UUID token) {
    return jpa.findByDownloadToken(token).map(mapper::toDomain);
  }

  @Override
  public Optional<Purchase> findByStripePaymentIntentId(String stripePaymentIntentId) {
    return jpa.findByStripePaymentIntentId(stripePaymentIntentId).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public int markPaid(String stripePaymentIntentId, Instant paidAt) {
    return jpa.markPaid(stripePaymentIntentId, paidAt);
  }

  @Override
  @Transactional
  public int markFailed(String stripePaymentIntentId) {
    return jpa.markFailed(stripePaymentIntentId);
  }

  @Override
  public List<Purchase> findPendingOlderThan(Instant cutoff) {
    return jpa.findPendingOlderThan(cutoff).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Purchase> findPaidWithoutEmail(Instant graceCutoff) {
    return jpa.findPaidWithoutEmail(graceCutoff).stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional
  public void updateEmailSentAt(UUID purchaseId, Instant emailSentAt) {
    jpa.updateEmailSentAt(purchaseId, emailSentAt);
  }
}
