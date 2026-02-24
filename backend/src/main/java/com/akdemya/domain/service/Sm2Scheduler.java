package com.akdemya.domain.service;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;

import java.time.LocalDateTime;

/**
 * Simplified SM-2 scheduler for flashcards.
 */
public class Sm2Scheduler {

  private static final double MIN_EASE_FACTOR = 1.3;

  public Result schedule(FlashcardReview review, ReviewGrade grade, LocalDateTime reviewedAt) {
    if (review == null) throw new IllegalArgumentException("review cannot be null");
    if (grade == null) throw new IllegalArgumentException("grade cannot be null");
    if (reviewedAt == null) throw new IllegalArgumentException("reviewedAt cannot be null");

    int intervalBefore = review.getIntervalDays();
    double easeBefore = review.getEaseFactor();

    int repetitions = review.getRepetitions();
    int lapses = review.getLapses();
    double easeAfter = easeBefore;
    int intervalAfter;
    ReviewState stateAfter;

    switch (grade) {
      case AGAIN -> {
        lapses += 1;
        repetitions = 0;
        easeAfter = clampEase(easeBefore - 0.20);
        intervalAfter = 1;
        stateAfter = ReviewState.LEARNING;
      }
      case HARD -> {
        repetitions += 1;
        easeAfter = clampEase(easeBefore - 0.15);
        intervalAfter = Math.max(1, (int) Math.round(intervalBefore * 1.2));
        stateAfter = repetitions >= 2 ? ReviewState.REVIEW : ReviewState.LEARNING;
      }
      case GOOD -> {
        repetitions += 1;
        intervalAfter = computeGoodInterval(repetitions, intervalBefore, easeBefore);
        stateAfter = repetitions >= 2 ? ReviewState.REVIEW : ReviewState.LEARNING;
      }
      case EASY -> {
        repetitions += 1;
        easeAfter = clampEase(easeBefore + 0.15);
        intervalAfter = (int) Math.round(intervalBefore * easeBefore * 1.3);
        stateAfter = repetitions >= 2 ? ReviewState.REVIEW : ReviewState.LEARNING;
      }
      default -> throw new IllegalStateException("Unexpected grade: " + grade);
    }

    LocalDateTime dueAt = reviewedAt.plusDays(intervalAfter);

    return new Result(
        stateAfter,
        repetitions,
        lapses,
        intervalBefore,
        intervalAfter,
        easeBefore,
        easeAfter,
        dueAt
    );
  }

  private int computeGoodInterval(int repetitions, int intervalBefore, double easeBefore) {
    if (repetitions == 1) return 1;
    if (repetitions == 2) return 3;
    return (int) Math.round(intervalBefore * easeBefore);
  }

  private double clampEase(double ease) {
    return Math.max(ease, MIN_EASE_FACTOR);
  }

  public static class Result {
    private final ReviewState state;
    private final int repetitions;
    private final int lapses;
    private final int intervalBefore;
    private final int intervalAfter;
    private final double easeBefore;
    private final double easeAfter;
    private final LocalDateTime dueAt;

    public Result(ReviewState state,
                  int repetitions,
                  int lapses,
                  int intervalBefore,
                  int intervalAfter,
                  double easeBefore,
                  double easeAfter,
                  LocalDateTime dueAt) {
      this.state = state;
      this.repetitions = repetitions;
      this.lapses = lapses;
      this.intervalBefore = intervalBefore;
      this.intervalAfter = intervalAfter;
      this.easeBefore = easeBefore;
      this.easeAfter = easeAfter;
      this.dueAt = dueAt;
    }

    public ReviewState getState() { return state; }
    public int getRepetitions() { return repetitions; }
    public int getLapses() { return lapses; }
    public int getIntervalBefore() { return intervalBefore; }
    public int getIntervalAfter() { return intervalAfter; }
    public double getEaseBefore() { return easeBefore; }
    public double getEaseAfter() { return easeAfter; }
    public LocalDateTime getDueAt() { return dueAt; }
  }
}
