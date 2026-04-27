package com.akdemya.domain.port.in;

import java.util.UUID;

/**
 * Use case for creating a Stripe PaymentIntent for a digital product purchase.
 *
 * <p>Domain inbound port — pure Java, no framework dependencies.</p>
 */
public interface CreatePaymentIntentUseCase {

  /**
   * Command to create a payment intent.
   *
   * @param email     buyer email (must not be blank)
   * @param productId catalog SKU of the digital product (must not be blank)
   * @param userId    optional logged-in user id; {@code null} for anonymous checkout
   */
  record Command(String email, String productId, UUID userId) {}

  /**
   * Result of creating a payment intent.
   *
   * @param clientSecret  Stripe client secret returned to the frontend
   * @param downloadToken UUID used to access the purchase via the download endpoints
   */
  record Result(String clientSecret, UUID downloadToken) {}

  Result createIntent(Command command);
}
