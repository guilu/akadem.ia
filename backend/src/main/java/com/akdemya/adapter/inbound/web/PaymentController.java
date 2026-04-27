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
        // TODO Phase 9 (task 9.2): accept @Valid @RequestBody CreateIntentRequest and
        // return CreateIntentResponse{clientSecret, downloadToken}. The temporary call
        // below keeps the controller wired to the new use case signature so the build
        // stays green; the final Phase 9 refactor replaces this with the real flow.
        CreatePaymentIntentUseCase.Result result = createPaymentIntentUseCase.createIntent(
                new CreatePaymentIntentUseCase.Command(null, null, null));
        return ResponseEntity.ok(new PaymentIntentResponse(result.clientSecret()));
    }
}
