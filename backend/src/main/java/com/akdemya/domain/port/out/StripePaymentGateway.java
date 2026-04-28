package com.akdemya.domain.port.out;

import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port that delegates Stripe-specific PaymentIntent creation to an
 * infrastructure adapter, keeping {@code PurchaseService} free of Stripe SDK
 * dependencies.
 *
 * <p>The application service pre-allocates {@code purchaseId} and
 * {@code downloadToken} so they can be stamped on the PaymentIntent metadata
 * (used by the webhook handler and the reconciliation scheduler to look up
 * the originating {@code Purchase}). Persistence of the {@code Purchase}
 * aggregate happens AFTER this call returns successfully — see
 * {@code design.md §5}.</p>
 */
public interface StripePaymentGateway {

  /**
   * Result of creating a PaymentIntent.
   *
   * @param clientSecret    Stripe client secret returned to the frontend
   * @param paymentIntentId Stripe PaymentIntent identifier (e.g. {@code pi_xxx});
   *                        persisted on the {@code Purchase} aggregate
   */
  record GatewayResult(String clientSecret, String paymentIntentId) {}

  /**
   * Read-only snapshot of a Stripe PaymentIntent — the minimum fields the
   * reconciliation scheduler needs to decide whether the originating
   * {@code Purchase} should transition to PAID, FAILED, or stay PENDING.
   *
   * <p>Kept SDK-free on purpose: the domain port returns a plain Java record so
   * it stays mockable without the Stripe SDK leaking into the domain or
   * application layers.</p>
   *
   * @param id     Stripe PaymentIntent identifier
   * @param status Stripe PaymentIntent status string (e.g. {@code "succeeded"},
   *               {@code "canceled"}, {@code "payment_failed"},
   *               {@code "processing"}, {@code "requires_action"})
   */
  record StripePaymentIntentSnapshot(String id, String status) {}

  /**
   * Creates a Stripe PaymentIntent for the product referenced by
   * {@code command.productId()}. The implementation MUST attach metadata
   * {@code {purchaseId, downloadToken, productId, email, userId?}} so the
   * webhook handler and reconciliation scheduler can resolve the originating
   * purchase.
   *
   * @throws IllegalArgumentException if the productId is unknown to the catalog
   * @throws RuntimeException         on any Stripe SDK failure
   */
  GatewayResult createPaymentIntent(CreatePaymentIntentUseCase.Command command,
                                    UUID purchaseId,
                                    UUID downloadToken);

  /**
   * Retrieves the latest {@link StripePaymentIntentSnapshot} for a previously
   * created PaymentIntent. Used by the reconciliation scheduler to settle
   * orphan {@code PENDING} purchases when the webhook never arrived.
   *
   * @param paymentIntentId Stripe PaymentIntent identifier (e.g. {@code pi_xxx})
   * @return the snapshot, or {@link Optional#empty()} if Stripe did not return
   *         a PaymentIntent for the supplied id
   * @throws RuntimeException on any Stripe SDK failure
   */
  Optional<StripePaymentIntentSnapshot> retrieve(String paymentIntentId);
}
