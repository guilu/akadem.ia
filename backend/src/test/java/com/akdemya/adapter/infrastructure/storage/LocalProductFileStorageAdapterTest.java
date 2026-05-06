package com.akdemya.adapter.infrastructure.storage;

import com.akdemya.application.config.ProductsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalProductFileStorageAdapterTest {

  @Test
  void open_returnsInputStreamWhenFileExists(@TempDir Path tmpDir) throws IOException {
    byte[] payload = "%PDF-1.4 dummy bytes".getBytes();
    Path file = tmpDir.resolve("temario-subalterno-gva.pdf");
    Files.write(file, payload);

    LocalProductFileStorageAdapter adapter = newAdapter(tmpDir);

    try (InputStream stream = adapter.open("temario-subalterno-gva.pdf")) {
      assertArrayEquals(payload, stream.readAllBytes());
    }
  }

  @Test
  void open_throwsRuntimeExceptionWhenFileMissing(@TempDir Path tmpDir) {
    LocalProductFileStorageAdapter adapter = newAdapter(tmpDir);

    assertThrows(RuntimeException.class, () -> adapter.open("missing-file.pdf"));
  }

  @Test
  void open_sanitizesPathPreventingTraversal(@TempDir Path tmpDir) throws IOException {
    byte[] payload = "%PDF-1.4 sanitized".getBytes();
    Path file = tmpDir.resolve("temario-subalterno-gva.pdf");
    Files.write(file, payload);

    LocalProductFileStorageAdapter adapter = newAdapter(tmpDir);

    // Sanitization must strip parent dirs; "../temario-subalterno-gva.pdf" must
    // resolve to the file in tmpDir.
    try (InputStream stream = adapter.open("../temario-subalterno-gva.pdf")) {
      assertArrayEquals(payload, stream.readAllBytes());
    }
  }

  private LocalProductFileStorageAdapter newAdapter(Path storage) {
    ProductsProperties props = new ProductsProperties();
    props.setStoragePath(storage.toString());
    return new LocalProductFileStorageAdapter(props);
  }
}
