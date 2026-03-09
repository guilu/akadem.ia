package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.in.GenerateQuizUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/quizzes")
public class QuizGenerationController {

    private final GenerateQuizUseCase useCase;

    public QuizGenerationController(GenerateQuizUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            req.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }

        GenerateQuizUseCase.GenerateQuizCommand command = new GenerateQuizUseCase.GenerateQuizCommand(
                req.sourceId(),
                req.unitId(),
                req.topic(),
                req.difficulty(),
                req.questionCount() != null ? req.questionCount() : 10,
                req.includeHints() != null ? req.includeHints() : false,
                req.storeAsDraft() != null ? req.storeAsDraft() : true
        );

        try {
            GenerateQuizUseCase.GenerateQuizResult result = useCase.generate(command);
            return ResponseEntity.ok(new GenerateResponse(
                    result.drafts().size(),
                    result.drafts().stream().map(this::toResponse).toList()
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/drafts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DraftResponse> drafts(@RequestParam UUID sourceId) {
        return useCase.listDrafts(sourceId).stream().map(this::toResponse).toList();
    }

    private DraftResponse toResponse(GeneratedQuestionDraft d) {
        return new DraftResponse(
                d.getId(), d.getSourceDocumentId(), d.getUnitId(),
                d.getTopic(), d.getDifficulty(), d.getStatement(),
                d.getAnswers(), d.getCorrectIndex(),
                d.getHint(), d.getExplanation(), d.getReference(),
                d.getCreatedAt(), d.getStatus().name()
        );
    }

    record GenerateRequest(
            UUID sourceId,
            UUID unitId,
            String topic,
            Question.Difficulty difficulty,
            Integer questionCount,
            Boolean includeHints,
            Boolean storeAsDraft
    ) {
        void validate() {
            if (sourceId == null) throw new IllegalArgumentException("sourceId is required");
            if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
            if (difficulty == null) throw new IllegalArgumentException("difficulty is required");
            if (questionCount != null && (questionCount < 1 || questionCount > 50)) {
                throw new IllegalArgumentException("questionCount must be between 1 and 50");
            }
        }
    }

    record GenerateResponse(int generated, List<DraftResponse> questions) {}

    record DraftResponse(
            UUID id, UUID sourceDocumentId, UUID unitId,
            String topic, String difficulty, String statement,
            List<String> answers, int correctIndex,
            String hint, String explanation, String reference,
            LocalDateTime createdAt, String status
    ) {}
}
