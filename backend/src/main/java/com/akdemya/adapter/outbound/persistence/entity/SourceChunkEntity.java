package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "source_chunks")
public class SourceChunkEntity {

    @Id
    private UUID id;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(columnDefinition = "text")
    private String metadata;

    // Stored as JSON float array string for V1.
    // Upgrade path: migrate to vector(N) column and switch VectorSearchPort adapter.
    @Column(columnDefinition = "text")
    private String embedding;

    public SourceChunkEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(UUID sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
}
