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
  void intervalHintsForNewCard() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);

    var hints = new Sm2Scheduler().intervalHints(review, now);

    assertEquals("<1m", hints.again());
    assertEquals("<10m", hints.good());
    assertEquals("4d", hints.easy());
  }

  @Test
  void newAgainSchedulesFirstLearningStep() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.AGAIN, now);

    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(0, result.getLearningStepAfter());
    assertEquals(now.plusMinutes(1), result.getDueAt());
  }

  @Test
  void newGoodMovesToSecondLearningStep() {
    var review = FlashcardReview.createNew(userId, flashcardId, now);

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);

    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(1, result.getLearningStepAfter());
    assertEquals(now.plusMinutes(10), result.getDueAt());
  }

  @Test
  void learningLastStepGoodPromotesToReview() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 0, 1, 1, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);

    assertEquals(ReviewState.REVIEW, result.getState());
    assertEquals(0, result.getLearningStepAfter());
    assertEquals(now.plusDays(1), result.getDueAt());
  }

  @Test
  void easyFromLearningJumpsToReviewWithEasyInterval() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.0, 0, 0, 0, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.EASY, now);

    assertEquals(ReviewState.REVIEW, result.getState());
    assertEquals(now.plusDays(4), result.getDueAt());
  }

  @Test
  void reviewAgainRelearnsInMinutes() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 1.4, 5, 0, 3, 1,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.AGAIN, now);

    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(0, result.getLearningStepAfter());
    assertEquals(now.plusMinutes(10), result.getDueAt());
  }

  @Test
  void clampEaseFactorMinimumOnAgain() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 1.35, 5, 0, 3, 1,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));

    var result = new Sm2Scheduler().schedule(review, ReviewGrade.AGAIN, now);

    assertEquals(1.3, result.getEaseAfter());
  }
}
