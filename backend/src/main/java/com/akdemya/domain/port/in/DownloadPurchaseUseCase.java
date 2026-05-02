package com.akdemya.domain.port.in;

import com.akdemya.domain.model.DigitalProduct;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Use case for opening the digital product file associated with a purchased download token.
 *
 * <p>Domain inbound port — pure Java, no framework dependencies.</p>
 */
public interface DownloadPurchaseUseCase {

  /**
   * Resolved download payload: the product metadata (used for filename / content-type)
   * and an open {@link InputStream} positioned at the beginning of the file. The caller
   * is responsible for closing the stream.
   */
  record Result(DigitalProduct product, InputStream stream) {}

  /**
   * Open the file for the purchase identified by {@code downloadToken}.
   *
   * @param downloadToken unique token assigned to the purchase at create-intent time
   * @return product metadata + open stream
   * @throws NoSuchElementException if the token is unknown or the purchase status is not PAID
   */
  Result openByToken(UUID downloadToken);
}
