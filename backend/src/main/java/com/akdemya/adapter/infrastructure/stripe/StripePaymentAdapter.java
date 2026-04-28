package com.akdemya.adapter.infrastructure.stripe;

import com.akdemya.application.config.StripeProperties;
import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.akdemya.domain.port.out.ProductCatalog;
import com.akdemya.domain.port.out.StripePaymentGateway;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stripe-backed implementation of {@link StripePaymentGateway}.
 *
 * <p>Builds a {@link PaymentIntent} for the product resolved via
 * {@link ProductCatalog} and stamps metadata
 * {@code {purchaseId, downloadToken, productId, email, userId?}} so the
 * webhook handler and reconciliation scheduler can resolve the originating
 * purchase. Pricing comes from {@link DigitalProduct} — no hard-coded amount.</p>
 */
@Component
public class StripePaymentAdapter implements StripePaymentGateway {

  private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

  private final StripeProperties stripeProperties;
  private final ProductCatalog productCatalog;

  public StripePaymentAdapter(StripeProperties stripeProperties, ProductCatalog productCatalog) {
    this.stripeProperties = stripeProperties;
    this.productCatalog = productCatalog;
  }

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeProperties.getSecretKey();
  }

  @Override
  public GatewayResult createPaymentIntent(CreatePaymentIntentUseCase.Command command,
                                           UUID purchaseId,
                                           UUID downloadToken) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (purchaseId == null) throw new IllegalArgumentException("purchaseId cannot be null");
    if (downloadToken == null) throw new IllegalArgumentException("downloadToken cannot be null");

    DigitalProduct product = productCatalog.findById(command.productId())
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown productId: " + command.productId()));

    PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
        .setAmount(product.getAmountCents())
        .setCurrency(product.getCurrency())
        .putMetadata("purchaseId", purchaseId.toString())
        .putMetadata("downloadToken", downloadToken.toString())
        .putMetadata("productId", product.getSku())
        .putMetadata("email", command.email());

    if (command.userId() != null) {
      builder.putMetadata("userId", command.userId().toString());
    }

    try {
      PaymentIntent intent = PaymentIntent.create(builder.build());
      return new GatewayResult(intent.getClientSecret(), intent.getId());
    } catch (StripeException ex) {
      log.error("Stripe PaymentIntent.create failed for productId={} purchaseId={}: {}",
          product.getSku(), purchaseId, ex.getMessage(), ex);
      throw new RuntimeException("Failed to create Stripe PaymentIntent", ex);
    }
  }
}
