package com.akdemya.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIntentRequest(
    @NotBlank String email,
    @NotBlank String productId
) {}
