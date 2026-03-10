package com.akdemya.domain.model;

import java.util.UUID;

public class SourceChunk {

    private final UUID id;
    private final UUID sourceDocumentId;
    private final String content;
    private final int chunkIndex;
    private final String metadata; // JSON: {"article":"62","section":"Artículo","sourceDocumentId":"..."}
    private final String unitName; // Full heading line captured during chunking, e.g. "Artículo 62. Del rey"
    private final UUID unitId;    // Set after user confirms indexing; FK → units(id)

    public SourceChunk(UUID id, UUID sourceDocumentId, String content,
                       int chunkIndex, String metadata, String unitName, UUID unitId) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.metadata = metadata;
        this.unitName = unitName;
        this.unitId = unitId;
    }

    public static SourceChunk create(UUID sourceDocumentId, String content,
                                     int chunkIndex, String metadata) {
        return new SourceChunk(UUID.randomUUID(), sourceDocumentId, content, chunkIndex, metadata, null, null);
    }

    public static SourceChunk create(UUID sourceDocumentId, String content,
                                     int chunkIndex, String metadata, String unitName) {
        return new SourceChunk(UUID.randomUUID(), sourceDocumentId, content, chunkIndex, metadata, unitName, null);
    }

    public SourceChunk withUnitId(UUID newUnitId) {
        return new SourceChunk(id, sourceDocumentId, content, chunkIndex, metadata, unitName, newUnitId);
    }

    public UUID getId() { return id; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public String getContent() { return content; }
    public int getChunkIndex() { return chunkIndex; }
    public String getMetadata() { return metadata; }
    public String getUnitName() { return unitName; }
    public UUID getUnitId() { return unitId; }
}
