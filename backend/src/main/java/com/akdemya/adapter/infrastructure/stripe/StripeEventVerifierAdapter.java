package com.akdemya.adapter.infrastructure.stripe;

import com.akdemya.application.config.StripeProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;

/**
 * Verifies the {@code Stripe-Signature} header against the raw payload using
 * the configured webhook secret.
 *
 * <p>Thin wrapper around {@link Webhook#constructEvent(String, String, String)}
 * so the rest of the application can depend on a domain-friendly seam instead
 * of static Stripe SDK calls (eases unit testing of {@code PurchaseService}).</p>
 */
@Component
public class StripeEventVerifierAdapter {

  private final StripeProperties stripeProperties;

  public StripeEventVerifierAdapter(StripeProperties stripeProperties) {
    this.stripeProperties = stripeProperties;
  }

  /**
   * Verifies the signature header and parses the Stripe event.
   *
   * @throws SignatureVerificationException if the signature is missing,
   *     malformed, or does not match the payload + webhook secret.
   */
  public Event constructEvent(String rawPayload, String signatureHeader)
      throws SignatureVerificationException {
    return Webhook.constructEvent(rawPayload, signatureHeader, stripeProperties.getWebhookSecret());
  }
}
