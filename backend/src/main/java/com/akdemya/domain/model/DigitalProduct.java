package com.akdemya.domain.model;

/**
 * Value object describing a downloadable digital product (e.g. a PDF temario).
 *
 * <p>Resolved by the {@code ProductCatalog} port from a {@code productId} (SKU) string.
 * Carries the pricing info used to build the Stripe PaymentIntent and the storage
 * coordinates used by {@code ProductFileStoragePort} to stream the file to the buyer.</p>
 */
public class DigitalProduct {

  private final String sku;
  private final String displayName;
  private final long amountCents;
  private final String currency;
  private final String storageKey;
  private final String displayFilename;

  public DigitalProduct(String sku,
                        String displayName,
                        long amountCents,
                        String currency,
                        String storageKey,
                        String displayFilename) {
    if (sku == null || sku.trim().isEmpty())
      throw new IllegalArgumentException("sku cannot be blank");
    if (displayName == null || displayName.trim().isEmpty())
      throw new IllegalArgumentException("displayName cannot be blank");
    if (amountCents <= 0) throw new IllegalArgumentException("amountCents must be positive");
    if (currency == null || currency.trim().isEmpty())
      throw new IllegalArgumentException("currency cannot be blank");
    if (storageKey == null || storageKey.trim().isEmpty())
      throw new IllegalArgumentException("storageKey cannot be blank");
    if (displayFilename == null || displayFilename.trim().isEmpty())
      throw new IllegalArgumentException("displayFilename cannot be blank");
    this.sku = sku;
    this.displayName = displayName;
    this.amountCents = amountCents;
    this.currency = currency;
    this.storageKey = storageKey;
    this.displayFilename = displayFilename;
  }

  public String getSku() { return sku; }
  public String getDisplayName() { return displayName; }
  public long getAmountCents() { return amountCents; }
  public String getCurrency() { return currency; }
  public String getStorageKey() { return storageKey; }
  public String getDisplayFilename() { return displayFilename; }
}
