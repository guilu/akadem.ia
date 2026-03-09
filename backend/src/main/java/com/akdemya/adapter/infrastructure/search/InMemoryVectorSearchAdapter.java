package com.akdemya.adapter.infrastructure.search;

import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.out.SourceChunkRepository;
import com.akdemya.domain.port.out.VectorSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * V1 vector search: loads all chunks for a document from the DB,
 * computes cosine similarity in-memory, returns top-K results.
 *
 * Upgrade path to pgvector:
 *   1. Add migration: ALTER TABLE source_chunks ADD COLUMN embedding_vec vector(1536)
 *   2. Backfill with: UPDATE source_chunks SET embedding_vec = embedding::vector
 *   3. Create index: CREATE INDEX ON source_chunks USING ivfflat (embedding_vec vector_cosine_ops)
 *   4. Replace this adapter with PgVectorSearchAdapter using a native @Query
 *   5. The VectorSearchPort interface stays unchanged
 */
@Component
public class InMemoryVectorSearchAdapter implements VectorSearchPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorSearchAdapter.class);

    private final SourceChunkRepository chunkRepo;

    public InMemoryVectorSearchAdapter(SourceChunkRepository chunkRepo) {
        this.chunkRepo = chunkRepo;
    }

    @Override
    public List<SourceChunk> findTopK(float[] queryVector, UUID sourceDocumentId, int topK) {
        List<SourceChunkRepository.ChunkWithEmbedding> all =
                chunkRepo.findWithEmbeddingsBySourceDocumentId(sourceDocumentId);

        if (all.isEmpty()) {
            log.warn("No chunks found for sourceDocumentId={}", sourceDocumentId);
            return List.of();
        }

        return all.stream()
                .filter(c -> c.embedding() != null && c.embedding().length > 0)
                .map(c -> new ScoredChunk(c.chunk(), cosineSimilarity(queryVector, c.embedding())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record ScoredChunk(SourceChunk chunk, double score) {}
}
