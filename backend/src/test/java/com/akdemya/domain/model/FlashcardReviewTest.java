package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardReviewTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID flashcardId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.now();

  // --- easeFactor ---

  @Test
  void easeFactorBelowMinimumThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.NEW, 1.2, 0, 0, 0, 0, now, null, now, now));
  }

  @Test
  void easeFactorAtExactMinimumIsAllowed() {
    assertDoesNotThrow(
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.NEW, 1.3, 0, 0, 0, 0, now, null, now, now));
  }

  // --- intervalDays ---

  @Test
  void negativeIntervalDaysThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.NEW, 2.5, -1, 0, 0, 0, now, null, now, now));
  }

  @Test
  void zeroIntervalDaysIsAllowed() {
    assertDoesNotThrow(
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.NEW, 2.5, 0, 0, 0, 0, now, null, now, now));
  }

  // --- repetitions ---

  @Test
  void negativeRepetitionsThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.LEARNING, 2.5, 1, 0, -1, 0, now, now, now, now));
  }

  // --- lapses ---

  @Test
  void negativeLapsesThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.REVIEW, 2.5, 7, 0, 3, -1, now, now, now, now));
  }

  // --- dueAt ---

  @Test
  void dueAtCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
            ReviewState.NEW, 2.5, 0, 0, 0, 0, null, null, now, now));
  }

  // --- createNew ---

  @Test
  void createNewInitialisesCorrectDefaults() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);
    assertEquals(ReviewState.NEW, review.getState());
    assertEquals(2.5, review.getEaseFactor());
    assertEquals(0, review.getIntervalDays());
    assertEquals(0, review.getLearningStep());
    assertEquals(0, review.getRepetitions());
    assertEquals(0, review.getLapses());
    assertEquals(now, review.getDueAt());
    assertNull(review.getLastReviewedAt());
    assertNotNull(review.getId());
  }

  // --- update ---

  @Test
  void updateAppliesNewSrsValues() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);
    LocalDateTime nextDue = now.plusDays(1);
    review.update(ReviewState.LEARNING, 2.3, 1, 0, 1, 0, nextDue);
    assertEquals(ReviewState.LEARNING, review.getState());
    assertEquals(2.3, review.getEaseFactor());
    assertEquals(1, review.getIntervalDays());
    assertEquals(1, review.getRepetitions());
    assertEquals(nextDue, review.getDueAt());
    assertNotNull(review.getLastReviewedAt());
  }

  @Test
  void updateRejectsInvalidEaseFactor() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);
    assertThrows(IllegalArgumentException.class,
        () -> review.update(ReviewState.LEARNING, 1.0, 1, 0, 1, 0, now.plusDays(1)));
  }
}
