package com.akdemya.domain.port.in;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FlashcardReviewUseCase {

  RegisterResponse registerReview(RegisterCommand command);

  record RegisterCommand(UUID userId, UUID flashcardId, ReviewGrade grade, LocalDateTime reviewedAt) {}

  record RegisterResponse(FlashcardReview review, FlashcardReviewLog log) {}
}
