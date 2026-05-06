package com.akdemya.adapter.inbound.web.dto;

/**
 * Buyer-safe view of a purchase used by {@code GET /api/v1/downloads/{token}/info}.
 *
 * <p>Exposes only fields safe to show to the buyer; never serialises
 * {@code stripePaymentIntentId}, internal user id, or wall-clock timestamps.</p>
 */
public record PurchaseInfoResponse(
    String email,
    String productName,
    String status,
    long amountCents,
    String currency
) {}
