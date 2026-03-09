package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceChunk;

import java.util.List;
import java.util.UUID;

/**
 * Retrieves the most semantically similar chunks to a query vector.
 * V1: in-memory cosine similarity. Upgrade path: pgvector native queries.
 */
public interface VectorSearchPort {
    List<SourceChunk> findTopK(float[] queryVector, UUID sourceDocumentId, int topK);
}
