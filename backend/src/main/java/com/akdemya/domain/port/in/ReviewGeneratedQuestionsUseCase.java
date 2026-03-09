package com.akdemya.domain.port.in;

import com.akdemya.domain.model.GeneratedQuestionDraft;

import java.util.List;
import java.util.UUID;

public interface ReviewGeneratedQuestionsUseCase {

    /**
     * List drafts for a source document, optionally filtered by status.
     * Passing null for status returns all drafts regardless of status.
     */
    List<GeneratedQuestionDraft> listDrafts(UUID sourceId, GeneratedQuestionDraft.Status status);

    /**
     * Approve a draft: publishes it to the question bank (creates Question + Answers),
     * then marks the draft as VALIDATED.
     */
    GeneratedQuestionDraft approve(UUID draftId);

    /**
     * Reject a draft: marks it as REJECTED without saving to the question bank.
     */
    GeneratedQuestionDraft reject(UUID draftId);
}
