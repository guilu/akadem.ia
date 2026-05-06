package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PurchaseEntity}.
 *
 * <p>Modifying queries used by the webhook handler ({@code markPaid} / {@code markFailed})
 * filter on {@code status='PENDING'} so a second delivery for an already-resolved
 * purchase returns {@code rowsAffected == 0} — providing idempotency at the DB level.</p>
 */
public interface JpaPurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {

  Optional<PurchaseEntity> findByDownloadToken(UUID downloadToken);

  Optional<PurchaseEntity> findByStripePaymentIntentId(String stripePaymentIntentId);

  @Query("SELECT p FROM PurchaseEntity p "
      + "WHERE p.status = 'PENDING' AND p.createdAt < :cutoff")
  List<PurchaseEntity> findPendingOlderThan(@Param("cutoff") Instant cutoff);

  @Query("SELECT p FROM PurchaseEntity p "
      + "WHERE p.status = 'PAID' AND p.emailSentAt IS NULL AND p.paidAt < :cutoff")
  List<PurchaseEntity> findPaidWithoutEmail(@Param("cutoff") Instant cutoff);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE PurchaseEntity p "
      + "SET p.status = 'PAID', p.paidAt = :paidAt "
      + "WHERE p.stripePaymentIntentId = :piId AND p.status = 'PENDING'")
  int markPaid(@Param("piId") String stripePaymentIntentId,
               @Param("paidAt") Instant paidAt);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE PurchaseEntity p "
      + "SET p.status = 'FAILED' "
      + "WHERE p.stripePaymentIntentId = :piId AND p.status = 'PENDING'")
  int markFailed(@Param("piId") String stripePaymentIntentId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE PurchaseEntity p "
      + "SET p.emailSentAt = :emailSentAt "
      + "WHERE p.id = :id")
  int updateEmailSentAt(@Param("id") UUID purchaseId,
                        @Param("emailSentAt") Instant emailSentAt);
}
