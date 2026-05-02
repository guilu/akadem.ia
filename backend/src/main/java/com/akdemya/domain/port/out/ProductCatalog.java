package com.akdemya.domain.port.out;

import com.akdemya.domain.model.DigitalProduct;

import java.util.Optional;

/**
 * Resolves a {@code productId} (SKU) to its {@link DigitalProduct} descriptor.
 *
 * <p>Implemented in-memory for the current MVP catalog; backed by a database
 * once the product range grows.</p>
 */
public interface ProductCatalog {

  Optional<DigitalProduct> findById(String productId);
}
