package com.akdemya.domain.port.in;

/**
 * Use case for handling a Stripe webhook event.
 *
 * <p>Receives the raw payload and the {@code Stripe-Signature} header so the
 * implementation can verify the HMAC signature before processing the event.</p>
 *
 * <p>Domain inbound port — pure Java, no framework dependencies.</p>
 */
public interface HandleStripeWebhookUseCase {

  /**
   * Verify and process a Stripe webhook event.
   *
   * @param rawPayload             raw request body bytes decoded as UTF-8 string
   * @param stripeSignatureHeader  value of the {@code Stripe-Signature} header
   */
  void handleEvent(String rawPayload, String stripeSignatureHeader);
}
