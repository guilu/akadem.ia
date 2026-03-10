package com.akdemya.domain.port.in;

import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;

import java.util.List;
import java.util.UUID;

public interface GenerateQuizUseCase {

    record GenerateQuizCommand(
            UUID sourceId,
            UUID unitId,
            Question.Difficulty difficulty,
            int questionCount,
            boolean includeHints,
            boolean storeAsDraft
    ) {
        public GenerateQuizCommand {
            if (sourceId == null) throw new IllegalArgumentException("sourceId is required");
            if (unitId == null) throw new IllegalArgumentException("unitId is required");
            if (difficulty == null) throw new IllegalArgumentException("difficulty is required");
            if (questionCount < 1 || questionCount > 50) throw new IllegalArgumentException("questionCount must be between 1 and 50");
        }
    }

    record GenerateQuizResult(List<GeneratedQuestionDraft> drafts) {}

    GenerateQuizResult generate(GenerateQuizCommand command);
}
