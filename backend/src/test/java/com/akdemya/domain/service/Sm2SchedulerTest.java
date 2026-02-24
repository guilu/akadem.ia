package com.akdemya.domain.service;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Sm2SchedulerTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID flashcardId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.of(2026, 2, 24, 12, 0);

  @Test
  void againResetsRepetitionsAndAddsLapse() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 1.4, 5, 3, 1,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.AGAIN, now);

    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(0, result.getRepetitions());
    assertEquals(2, result.getLapses());
    assertEquals(5, result.getIntervalBefore());
    assertEquals(1, result.getIntervalAfter());
    assertEquals(1.4, result.getEaseBefore());
    assertEquals(1.3, result.getEaseAfter());
    assertEquals(now.plusDays(1), result.getDueAt());
  }

  @Test
  void goodFirstSuccessSetsInitialInterval() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);

    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(1, result.getRepetitions());
    assertEquals(1, result.getIntervalAfter());
    assertEquals(now.plusDays(1), result.getDueAt());
  }

  @Test
  void goodSecondSuccessMovesToReview() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 1, 1, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);

    assertEquals(ReviewState.REVIEW, result.getState());
    assertEquals(2, result.getRepetitions());
    assertEquals(3, result.getIntervalAfter());
  }

  @Test
  void goodReviewScalesIntervalByEase() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 3, 3, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);

    assertEquals(8, result.getIntervalAfter());
  }

  @Test
  void hardScalesIntervalByFixedFactor() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 10, 3, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.HARD, now);

    assertEquals(12, result.getIntervalAfter());
    assertEquals(2.35, result.getEaseAfter());
  }

  @Test
  void easyScalesIntervalByEaseAndBonus() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.0, 5, 3, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.EASY, now);

    assertEquals(13, result.getIntervalAfter());
    assertEquals(2.15, result.getEaseAfter());
  }
}
