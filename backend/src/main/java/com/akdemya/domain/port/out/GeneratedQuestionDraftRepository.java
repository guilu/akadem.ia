package com.akdemya.domain.port.out;

import com.akdemya.domain.model.GeneratedQuestionDraft;

import java.util.List;
import java.util.UUID;

public interface GeneratedQuestionDraftRepository {
    GeneratedQuestionDraft save(GeneratedQuestionDraft draft);
    List<GeneratedQuestionDraft> saveAll(List<GeneratedQuestionDraft> drafts);
    List<GeneratedQuestionDraft> findBySourceDocumentId(UUID sourceDocumentId);
}
