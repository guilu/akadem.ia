package com.akdemya.domain.port.in;

import com.akdemya.domain.model.Answer;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;

import java.util.List;

public interface PublishGeneratedQuestionsUseCase {

    record PublishResult(Question question, List<Answer> answers) {}

    /**
     * Converts an approved draft into a Question + Answers persisted in the question bank.
     * The draft must have a non-null unitId.
     */
    PublishResult publish(GeneratedQuestionDraft draft);
}
