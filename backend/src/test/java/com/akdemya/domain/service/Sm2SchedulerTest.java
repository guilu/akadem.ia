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

  @Test
  void defaultConstructorUsesDefaultLearningSteps() {
    // Exercises the no-arg constructor; default steps are [1, 10] minutes
    // NEW + GOOD moves learningStep from 0 to 1, then dueAt = now + steps[1] = 10 minutes
    Sm2Scheduler scheduler = new Sm2Scheduler();
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.NEW, 2.5, 0, 0, 0, 0,
        now, now, now, now);
    var result = scheduler.schedule(review, ReviewGrade.GOOD, now);
    // Moves to next step index=1, which is 10 minutes
    assertEquals(now.plusMinutes(10), result.getDueAt());
  }

  @Test
  void reviewGoodRepetition1GivesInterval1() {
    // Tests computeGoodInterval(repetitions=1) branch
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 1, 0, 0, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);
    assertEquals(1, result.getIntervalAfter());
  }

  @Test
  void reviewGoodRepetition2GivesInterval3() {
    // Tests computeGoodInterval(repetitions=2) branch
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 3, 0, 1, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.GOOD, now);
    assertEquals(3, result.getIntervalAfter());
  }

  @Test
  void reviewHardReducesEaseAndScalesInterval() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 10, 0, 5, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.HARD, now);
    assertEquals(ReviewState.REVIEW, result.getState());
    assertEquals(2.35, result.getEaseAfter(), 0.01);
    assertEquals(12, result.getIntervalAfter()); // round(10 * 1.2)
  }

  @Test
  void reviewEasyIncreasesEaseAndScalesInterval() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.REVIEW, 2.5, 10, 0, 5, 0,
        now, now.minusDays(1), now.minusDays(10), now.minusDays(1));
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.EASY, now);
    assertEquals(ReviewState.REVIEW, result.getState());
    assertEquals(2.65, result.getEaseAfter(), 0.01);
  }

  @Test
  void learningHardStaysAtSameStepWithReducedEase() {
    var review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 0, 0, 1, 0,
        now, now.minusMinutes(1), now.minusDays(1), now.minusMinutes(1));
    var result = new Sm2Scheduler().schedule(review, ReviewGrade.HARD, now);
    assertEquals(ReviewState.LEARNING, result.getState());
    assertEquals(2.35, result.getEaseAfter(), 0.01);
  }
}
