package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.GeneratedQuestionDraftMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataGeneratedQuestionDraftRepository;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.port.out.GeneratedQuestionDraftRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GeneratedQuestionDraftPersistenceAdapter implements GeneratedQuestionDraftRepository {

    private final SpringDataGeneratedQuestionDraftRepository repository;
    private final GeneratedQuestionDraftMapper mapper;

    public GeneratedQuestionDraftPersistenceAdapter(SpringDataGeneratedQuestionDraftRepository repository,
                                                     GeneratedQuestionDraftMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public GeneratedQuestionDraft save(GeneratedQuestionDraft draft) {
        return mapper.toDomain(repository.save(mapper.toEntity(draft)));
    }

    @Override
    public List<GeneratedQuestionDraft> saveAll(List<GeneratedQuestionDraft> drafts) {
        return repository.saveAll(drafts.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GeneratedQuestionDraft> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GeneratedQuestionDraft> findBySourceDocumentId(UUID sourceDocumentId, GeneratedQuestionDraft.Status status) {
        if (status == null) {
            return repository.findBySourceDocumentIdOrderByCreatedAtDesc(sourceDocumentId)
                    .stream().map(mapper::toDomain).toList();
        }
        return repository.findBySourceDocumentIdAndStatusOrderByCreatedAtDesc(sourceDocumentId, status.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public GeneratedQuestionDraft updateStatus(UUID id, GeneratedQuestionDraft.Status newStatus) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Draft not found: " + id));
        entity.setStatus(newStatus.name());
        return mapper.toDomain(repository.save(entity));
    }
}
