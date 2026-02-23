package com.akdemya.domain.port.out;

import com.akdemya.domain.model.FlashcardReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardReviewRepository {

  Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

  /** Returns all due cards for a user ordered by dueAt ascending. */
  List<FlashcardReview> findDueByUserId(UUID userId);

  FlashcardReview save(FlashcardReview review);
}
