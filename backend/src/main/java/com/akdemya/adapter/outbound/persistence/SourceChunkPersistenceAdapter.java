package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SourceChunkEntity;
import com.akdemya.adapter.outbound.persistence.mapper.SourceChunkMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSourceChunkRepository;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.out.SourceChunkRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SourceChunkPersistenceAdapter implements SourceChunkRepository {

    private final SpringDataSourceChunkRepository repository;
    private final SourceChunkMapper mapper;

    public SourceChunkPersistenceAdapter(SpringDataSourceChunkRepository repository,
                                          SourceChunkMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SourceChunk save(SourceChunk chunk, float[] embedding) {
        SourceChunkEntity entity = mapper.toEntity(chunk, embedding);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<SourceChunk> findBySourceDocumentId(UUID sourceDocumentId) {
        return repository.findBySourceDocumentIdOrderByChunkIndex(sourceDocumentId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ChunkWithEmbedding> findWithEmbeddingsBySourceDocumentId(UUID sourceDocumentId) {
        return repository.findBySourceDocumentIdOrderByChunkIndex(sourceDocumentId)
                .stream()
                .map(e -> new ChunkWithEmbedding(mapper.toDomain(e), mapper.embeddingFromEntity(e)))
                .toList();
    }
}
