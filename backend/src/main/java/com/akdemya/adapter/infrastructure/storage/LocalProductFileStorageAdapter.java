package com.akdemya.adapter.infrastructure.storage;

import com.akdemya.application.config.ProductsProperties;
import com.akdemya.domain.port.out.ProductFileStoragePort;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Filesystem-backed {@link ProductFileStoragePort} reading product files from
 * {@link ProductsProperties#getStoragePath()}.
 *
 * <p>Path traversal is prevented by sanitising the {@code storageKey} with
 * {@code Paths.get(storageKey).getFileName()} before resolving against the
 * configured base directory.</p>
 */
@Component
public class LocalProductFileStorageAdapter implements ProductFileStoragePort {

  private final ProductsProperties props;

  public LocalProductFileStorageAdapter(ProductsProperties props) {
    this.props = props;
  }

  @Override
  public InputStream open(String storageKey) {
    if (storageKey == null || storageKey.isBlank()) {
      throw new RuntimeException("storageKey cannot be blank");
    }
    Path sanitized = Paths.get(storageKey).getFileName();
    if (sanitized == null) {
      throw new RuntimeException("Invalid storageKey: " + storageKey);
    }
    Path target = Paths.get(props.getStoragePath()).resolve(sanitized.toString());
    try {
      return new FileInputStream(target.toFile());
    } catch (Exception e) {
      throw new RuntimeException("Failed to open product file: " + sanitized, e);
    }
  }
}
