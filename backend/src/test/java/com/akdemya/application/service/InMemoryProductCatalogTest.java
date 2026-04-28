package com.akdemya.application.service;

import com.akdemya.domain.model.DigitalProduct;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProductCatalogTest {

  private final InMemoryProductCatalog catalog = new InMemoryProductCatalog();

  @Test
  void findById_returnsTemarioSubalternoGva() {
    Optional<DigitalProduct> result = catalog.findById("TEMARIO_SUBALTERNO_GVA");

    assertTrue(result.isPresent());
    DigitalProduct product = result.get();
    assertEquals("TEMARIO_SUBALTERNO_GVA", product.getSku());
    assertEquals("Temario Subalterno GVA", product.getDisplayName());
    assertEquals(1500L, product.getAmountCents());
    assertEquals("eur", product.getCurrency());
    assertEquals("temario-subalterno-gva.pdf", product.getStorageKey());
    assertEquals("Temario Subalterno GVA.pdf", product.getDisplayFilename());
  }

  @Test
  void findById_unknownSkuReturnsEmpty() {
    assertFalse(catalog.findById("UNKNOWN").isPresent());
  }
}
