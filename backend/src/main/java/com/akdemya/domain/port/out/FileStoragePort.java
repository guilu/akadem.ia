package com.akdemya.domain.port.out;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStoragePort {
    /** Stores the given bytes and returns the resolved storage path. */
    Path store(String filename, byte[] bytes);

    InputStream open(String storagePath);
}
