package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceChunk;

import java.util.List;
import java.util.UUID;

public interface SourceChunkRepository {
    SourceChunk save(SourceChunk chunk, float[] embedding);
    List<SourceChunk> findBySourceDocumentId(UUID sourceDocumentId);
    /** Returns chunks with their embeddings for vector search. */
    List<ChunkWithEmbedding> findWithEmbeddingsBySourceDocumentId(UUID sourceDocumentId);

    record ChunkWithEmbedding(SourceChunk chunk, float[] embedding) {}
}
