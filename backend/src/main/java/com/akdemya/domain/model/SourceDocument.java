package com.akdemya.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SourceDocument {

    private final UUID id;
    private final UUID subjectId;
    private final String name;
    private final String type;
    private final String version;
    private final String checksum;
    private final LocalDateTime uploadedAt;
    private final Status status;
    private final String storagePath;

    public enum Status {
        UPLOADED, PENDING_REVIEW, PROCESSED, FAILED
    }

    public SourceDocument(UUID id, UUID subjectId, String name, String type, String version,
                          String checksum, LocalDateTime uploadedAt,
                          Status status, String storagePath) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
        this.type = type;
        this.version = version;
        this.checksum = checksum;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.storagePath = storagePath;
    }

    public static SourceDocument create(UUID subjectId, String name, String type, String version,
                                        String checksum, String storagePath) {
        return new SourceDocument(
                UUID.randomUUID(), subjectId, name, type, version,
                checksum, LocalDateTime.now(),
                Status.PENDING_REVIEW, storagePath
        );
    }

    public SourceDocument withStatus(Status newStatus) {
        return new SourceDocument(id, subjectId, name, type, version, checksum, uploadedAt, newStatus, storagePath);
    }

    public UUID getId() { return id; }
    public UUID getSubjectId() { return subjectId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getVersion() { return version; }
    public String getChecksum() { return checksum; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public Status getStatus() { return status; }
    public String getStoragePath() { return storagePath; }
}
