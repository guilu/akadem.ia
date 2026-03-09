package com.akdemya.adapter.infrastructure.storage;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.port.out.FileStoragePort;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final RagProperties props;

    public LocalFileStorageAdapter(RagProperties props) {
        this.props = props;
    }

    @Override
    public Path store(String filename, byte[] bytes) {
        try {
            Path base = Paths.get(props.getStoragePath());
            Files.createDirectories(base);
            String sanitized = Paths.get(filename).getFileName().toString();
            Path target = base.resolve(UUID.randomUUID() + "_" + sanitized);
            Files.write(target, bytes);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }

    @Override
    public InputStream open(String storagePath) {
        try {
            return new FileInputStream(storagePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open file: " + storagePath, e);
        }
    }
}
