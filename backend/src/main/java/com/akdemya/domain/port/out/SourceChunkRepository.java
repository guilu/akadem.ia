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

    record ChunkWithEmbedding(SourceChunk chunk, float[] embedding) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkWithEmbedding that = (ChunkWithEmbedding) o;
            return java.util.Objects.equals(chunk, that.chunk) &&
                   java.util.Arrays.equals(embedding, that.embedding);
        }

        @Override
        public int hashCode() {
            int result = java.util.Objects.hash(chunk);
            result = 31 * result + java.util.Arrays.hashCode(embedding);
            return result;
        }

        @Override
        public String toString() {
            return "ChunkWithEmbedding[" +
                   "chunk=" + chunk + ", " +
                   "embedding=" + java.util.Arrays.toString(embedding) + "]";
        }
    }
}
