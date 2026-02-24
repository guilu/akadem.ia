package com.akdemya.domain.port.out;

import com.akdemya.domain.model.FlashcardReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardReviewRepository {

  Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

  /** Returns up to {@code limit} cards due for {@code userId} at or before {@code upTo}, ordered by dueAt asc. */
  List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit);

  FlashcardReview save(FlashcardReview review);
}
