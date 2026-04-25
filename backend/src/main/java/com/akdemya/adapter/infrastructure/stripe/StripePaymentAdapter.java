package com.akdemya.adapter.infrastructure.stripe;

import com.akdemya.application.config.StripeProperties;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
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
        Stripe.apiKey = stripeProperties.secretKey();
    }

    @Override
    public String createIntent() {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(1500L)
                    .setCurrency("eur")
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}
