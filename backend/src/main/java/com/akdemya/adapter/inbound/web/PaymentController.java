package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.CreateIntentRequest;
import com.akdemya.adapter.inbound.web.dto.CreateIntentResponse;
import com.akdemya.adapter.inbound.web.dto.WebhookResponse;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import com.akdemya.domain.port.in.HandleStripeWebhookUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentIntentUseCase createPaymentIntentUseCase;
    private final HandleStripeWebhookUseCase handleStripeWebhookUseCase;

    public PaymentController(CreatePaymentIntentUseCase createPaymentIntentUseCase,
                             HandleStripeWebhookUseCase handleStripeWebhookUseCase) {
        this.createPaymentIntentUseCase = createPaymentIntentUseCase;
        this.handleStripeWebhookUseCase = handleStripeWebhookUseCase;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<CreateIntentResponse> createIntent(@Valid @RequestBody CreateIntentRequest request) {
        CreatePaymentIntentUseCase.Result result = createPaymentIntentUseCase.createIntent(
            new CreatePaymentIntentUseCase.Command(request.email(), request.productId(), null));
        return ResponseEntity.ok(new CreateIntentResponse(result.clientSecret(), result.downloadToken()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> handleWebhook(@RequestBody byte[] payload,
                                                         @RequestHeader("Stripe-Signature") String stripeSignature) {
        handleStripeWebhookUseCase.handleEvent(new String(payload, StandardCharsets.UTF_8), stripeSignature);
        return ResponseEntity.ok(new WebhookResponse("ok"));
    }
}
