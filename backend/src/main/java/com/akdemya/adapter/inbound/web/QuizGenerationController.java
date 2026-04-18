package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.in.GenerateQuizUseCase;
import com.akdemya.domain.port.in.ReviewGeneratedQuestionsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class QuizGenerationController {

    private final GenerateQuizUseCase generateUseCase;
    private final ReviewGeneratedQuestionsUseCase reviewUseCase;

    public QuizGenerationController(GenerateQuizUseCase generateUseCase,
                                     ReviewGeneratedQuestionsUseCase reviewUseCase) {
        this.generateUseCase = generateUseCase;
        this.reviewUseCase = reviewUseCase;
    }

    @PostMapping("/api/ai/quizzes/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody GenerateRequest req) {
        try {
            req.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        GenerateQuizUseCase.GenerateQuizCommand command = new GenerateQuizUseCase.GenerateQuizCommand(
                req.sourceId(),
                req.unitId(),
                req.difficulty(),
                req.questionCount() != null ? req.questionCount() : 10,
                req.includeHints() != null ? req.includeHints() : false,
                req.storeAsDraft() != null ? req.storeAsDraft() : true
        );

        try {
            GenerateQuizUseCase.GenerateQuizResult result = generateUseCase.generate(command);
            return ResponseEntity.ok(new GenerateResponse(
                    result.drafts().size(),
                    result.drafts().stream().map(this::toResponse).toList()
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/ai/quizzes/drafts")
    public List<DraftResponse> drafts(
            @RequestParam UUID sourceId,
            @RequestParam(required = false) String status) {

        GeneratedQuestionDraft.Status statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = GeneratedQuestionDraft.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return reviewUseCase.listDrafts(sourceId, statusFilter).stream().map(this::toResponse).toList();
    }

    @PostMapping("/api/ai/drafts/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        try {
            GeneratedQuestionDraft updated = reviewUseCase.approve(id);
            return ResponseEntity.ok(toResponse(updated));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/ai/drafts/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        try {
            GeneratedQuestionDraft updated = reviewUseCase.reject(id);
            return ResponseEntity.ok(toResponse(updated));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
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
            Question.Difficulty difficulty,
            Integer questionCount,
            Boolean includeHints,
            Boolean storeAsDraft
    ) {
        void validate() {
            if (sourceId == null) throw new IllegalArgumentException("sourceId is required");
            if (unitId == null) throw new IllegalArgumentException("unitId is required");
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
