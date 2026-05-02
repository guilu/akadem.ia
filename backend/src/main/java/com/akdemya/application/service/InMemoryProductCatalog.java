package com.akdemya.application.service;

import com.akdemya.domain.model.DigitalProduct;
import com.akdemya.domain.port.out.ProductCatalog;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link ProductCatalog} for the MVP catalog (single SKU).
 *
 * <p>Once the catalog grows the {@link ProductCatalog} port can be backed by
 * a JPA-backed adapter without touching consumers.</p>
 */
@Component
public class InMemoryProductCatalog implements ProductCatalog {

  private final Map<String, DigitalProduct> products = Map.of(
      "TEMARIO_SUBALTERNO_GVA",
      new DigitalProduct(
          "TEMARIO_SUBALTERNO_GVA",
          "Temario Subalterno GVA",
          1500L,
          "eur",
          "temario-subalterno-gva.pdf",
          "Temario Subalterno GVA.pdf"
      )
  );

  @Override
  public Optional<DigitalProduct> findById(String productId) {
    if (productId == null) return Optional.empty();
    return Optional.ofNullable(products.get(productId));
  }
}
