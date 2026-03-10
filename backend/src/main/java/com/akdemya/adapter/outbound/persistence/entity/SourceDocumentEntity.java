package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_documents")
public class SourceDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 100)
    private String version;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;

    public SourceDocumentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
}
