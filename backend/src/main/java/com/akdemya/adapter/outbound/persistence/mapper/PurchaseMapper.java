package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.PurchaseEntity;
import com.akdemya.domain.model.Purchase;
import com.akdemya.domain.model.PurchaseStatus;
import org.springframework.stereotype.Component;

/**
 * Bidirectional mapper between the {@link Purchase} domain aggregate and its
 * {@link PurchaseEntity} JPA representation. Status is persisted as a {@code String}
 * to align with the DB CHECK constraint and decouple the domain enum from the schema.
 */
@Component
public class PurchaseMapper {

  public Purchase toDomain(PurchaseEntity e) {
    return new Purchase(
        e.getId(),
        e.getStripePaymentIntentId(),
        e.getDownloadToken(),
        e.getEmail(),
        e.getProductId(),
        e.getUserId(),
        PurchaseStatus.valueOf(e.getStatus()),
        e.getAmountCents(),
        e.getCurrency(),
        e.getCreatedAt(),
        e.getPaidAt(),
        e.getEmailSentAt()
    );
  }

  public PurchaseEntity toEntity(Purchase d) {
    PurchaseEntity e = new PurchaseEntity();
    e.setId(d.getId());
    e.setStripePaymentIntentId(d.getStripePaymentIntentId());
    e.setDownloadToken(d.getDownloadToken());
    e.setEmail(d.getEmail());
    e.setProductId(d.getProductId());
    e.setUserId(d.getUserId());
    e.setStatus(d.getStatus().name());
    e.setAmountCents(d.getAmountCents());
    e.setCurrency(d.getCurrency());
    e.setCreatedAt(d.getCreatedAt());
    e.setPaidAt(d.getPaidAt());
    e.setEmailSentAt(d.getEmailSentAt());
    return e;
  }
}
