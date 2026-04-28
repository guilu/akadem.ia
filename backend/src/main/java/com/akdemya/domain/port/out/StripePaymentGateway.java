package com.akdemya.domain.port.out;

import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;

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
}
