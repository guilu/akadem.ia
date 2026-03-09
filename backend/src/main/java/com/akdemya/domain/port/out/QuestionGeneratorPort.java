package com.akdemya.domain.port.out;

import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.in.GenerateQuizUseCase;

import java.util.List;

/**
 * Generates test questions from a set of context chunks using an LLM.
 * Implementations must return strictly structured, validated question data.
 */
public interface QuestionGeneratorPort {
    List<GeneratedQuestionDraft> generate(
            GenerateQuizUseCase.GenerateQuizCommand command,
            List<SourceChunk> contextChunks,
            String topic
    );
}
