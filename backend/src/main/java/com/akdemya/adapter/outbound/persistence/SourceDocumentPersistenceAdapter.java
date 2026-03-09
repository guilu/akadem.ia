package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.SourceDocumentMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSourceDocumentRepository;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.port.out.SourceDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SourceDocumentPersistenceAdapter implements SourceDocumentRepository {

    private final SpringDataSourceDocumentRepository repository;
    private final SourceDocumentMapper mapper;

    public SourceDocumentPersistenceAdapter(SpringDataSourceDocumentRepository repository,
                                             SourceDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SourceDocument save(SourceDocument document) {
        return mapper.toDomain(repository.save(mapper.toEntity(document)));
    }

    @Override
    public Optional<SourceDocument> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SourceDocument> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SourceDocument> findBySubjectId(UUID subjectId) {
        return repository.findBySubjectId(subjectId).stream().map(mapper::toDomain).toList();
    }
}
