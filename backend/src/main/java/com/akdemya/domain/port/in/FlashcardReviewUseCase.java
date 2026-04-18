package com.akdemya.domain.port.in;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FlashcardReviewUseCase {

  RegisterResponse registerReview(RegisterCommand command);

  List<HistoryItemResult> getReviewHistory(UUID userId, int limit);

  record RegisterCommand(UUID userId, UUID flashcardId, ReviewGrade grade, LocalDateTime reviewedAt) {}

  record RegisterResponse(FlashcardReview review, FlashcardReviewLog log) {}

  record HistoryItemResult(UUID id, UUID flashcardId, String front, String back,
                           ReviewGrade grade, LocalDateTime reviewedAt,
                           Integer intervalBefore, Integer intervalAfter,
                           Double easeBefore, Double easeAfter) {}
}
