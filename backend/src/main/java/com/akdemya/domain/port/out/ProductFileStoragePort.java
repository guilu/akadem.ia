package com.akdemya.domain.port.out;

import java.io.InputStream;

/**
 * Read-only access to the binary file backing a {@code DigitalProduct}.
 *
 * <p>Distinct from {@link FileStoragePort} (which manages user-uploaded source
 * documents) — this port only reads pre-provisioned product files keyed by an
 * opaque {@code storageKey} resolved from the product catalog.</p>
 */
public interface ProductFileStoragePort {

  /**
   * Opens an input stream for the file identified by {@code storageKey}.
   *
   * @throws RuntimeException if the file cannot be located or opened.
   */
  InputStream open(String storageKey);
}
