package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.PaymentIntentResponse;
import com.akdemya.domain.port.in.CreatePaymentIntentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentIntentUseCase createPaymentIntentUseCase;

    public PaymentController(CreatePaymentIntentUseCase createPaymentIntentUseCase) {
        this.createPaymentIntentUseCase = createPaymentIntentUseCase;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createIntent() {
        String clientSecret = createPaymentIntentUseCase.createIntent();
        return ResponseEntity.ok(new PaymentIntentResponse(clientSecret));
    }
}
