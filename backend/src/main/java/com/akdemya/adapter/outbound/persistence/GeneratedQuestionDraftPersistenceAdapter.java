package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.GeneratedQuestionDraftMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataGeneratedQuestionDraftRepository;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.port.out.GeneratedQuestionDraftRepository;
import org.springframework.stereotype.Component;

import java.util.List;
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
        List<com.akdemya.adapter.outbound.persistence.entity.GeneratedQuestionDraftEntity> entities =
                drafts.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GeneratedQuestionDraft> findBySourceDocumentId(UUID sourceDocumentId) {
        return repository.findBySourceDocumentIdOrderByCreatedAtDesc(sourceDocumentId)
                .stream().map(mapper::toDomain).toList();
    }
}
