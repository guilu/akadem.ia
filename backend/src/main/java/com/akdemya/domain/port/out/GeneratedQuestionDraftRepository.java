package com.akdemya.domain.port.out;

import com.akdemya.domain.model.GeneratedQuestionDraft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedQuestionDraftRepository {
    GeneratedQuestionDraft save(GeneratedQuestionDraft draft);
    List<GeneratedQuestionDraft> saveAll(List<GeneratedQuestionDraft> drafts);
    Optional<GeneratedQuestionDraft> findById(UUID id);

    /**
     * List drafts by source document. If status is null, returns all drafts regardless of status.
     */
    List<GeneratedQuestionDraft> findBySourceDocumentId(UUID sourceDocumentId, GeneratedQuestionDraft.Status status);

    GeneratedQuestionDraft updateStatus(UUID id, GeneratedQuestionDraft.Status newStatus);
}
