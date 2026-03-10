package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceChunk;

import java.util.List;
import java.util.UUID;

public interface SourceChunkRepository {
    /** Save chunk with embedding (used during legacy flow or future vector search). */
    SourceChunk save(SourceChunk chunk, float[] embedding);

    /** Save chunk without embedding (used during upload/preview step). */
    SourceChunk saveWithoutEmbedding(SourceChunk chunk);

    /** Update only the unit_id on an existing chunk. */
    void updateUnitId(UUID chunkId, UUID unitId);

    List<SourceChunk> findBySourceDocumentId(UUID sourceDocumentId);

    /** Returns chunks for a given unit, ordered by chunk_index. */
    List<SourceChunk> findByUnitId(UUID unitId);

    /** Returns chunks with their embeddings for vector search. */
    List<ChunkWithEmbedding> findWithEmbeddingsBySourceDocumentId(UUID sourceDocumentId);

    record ChunkWithEmbedding(SourceChunk chunk, float[] embedding) {}
}
