package com.akdemya.adapter.infrastructure.stripe;

import com.akdemya.application.config.StripeProperties;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentAdapter implements CreatePaymentIntentUseCase {

    private final StripeProperties stripeProperties;

    public StripePaymentAdapter(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    @Override
    public Result createIntent(Command command) {
        // TODO Phase 7 (task 7.1): resolve DigitalProduct via ProductCatalog, build
        // metadata (purchaseId/downloadToken/productId/email/userId), persist Purchase,
        // and return {clientSecret, downloadToken}. Temporary stub keeps the build green
        // while Phase 3 redefines the inbound port.
        throw new UnsupportedOperationException(
                "StripePaymentAdapter.createIntent(Command) not yet implemented — pending Phase 7");
    }
}
