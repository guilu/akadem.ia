package com.akdemya.domain.model;

import java.util.UUID;

public class SourceChunk {

    private final UUID id;
    private final UUID sourceDocumentId;
    private final String content;
    private final int chunkIndex;
    private final String metadata; // JSON: {"article":"62","section":"Título II","page":14}

    public SourceChunk(UUID id, UUID sourceDocumentId, String content,
                       int chunkIndex, String metadata) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.metadata = metadata;
    }

    public static SourceChunk create(UUID sourceDocumentId, String content,
                                     int chunkIndex, String metadata) {
        return new SourceChunk(UUID.randomUUID(), sourceDocumentId, content, chunkIndex, metadata);
    }

    public UUID getId() { return id; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public String getContent() { return content; }
    public int getChunkIndex() { return chunkIndex; }
    public String getMetadata() { return metadata; }
}
