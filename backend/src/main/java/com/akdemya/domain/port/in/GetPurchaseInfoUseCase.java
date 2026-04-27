package com.akdemya.domain.port.in;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Use case for retrieving buyer-safe metadata about a purchase by its download token.
 *
 * <p>Used by the post-payment download page to render purchase status (PAID / PENDING /
 * FAILED) without serving the file. Exposes only fields safe to show to the buyer
 * (never the Stripe PaymentIntent id, internal user id, or timestamps).</p>
 *
 * <p>Domain inbound port — pure Java, no framework dependencies.</p>
 */
public interface GetPurchaseInfoUseCase {

  /**
   * Buyer-safe view of a purchase.
   *
   * @param email        buyer email captured at create-intent time
   * @param productName  display name of the digital product (resolved via {@code ProductCatalog})
   * @param status       textual purchase status (PENDING / PAID / FAILED)
   * @param amountCents  product price in minor units
   * @param currency     ISO currency code (lower-case, e.g. {@code "eur"})
   */
  record PurchaseInfo(String email,
                      String productName,
                      String status,
                      long amountCents,
                      String currency) {}

  /**
   * Look up the purchase identified by {@code downloadToken}.
   *
   * @param downloadToken unique token assigned to the purchase at create-intent time
   * @return buyer-safe purchase info
   * @throws NoSuchElementException if the token is unknown
   */
  PurchaseInfo getInfo(UUID downloadToken);
}
