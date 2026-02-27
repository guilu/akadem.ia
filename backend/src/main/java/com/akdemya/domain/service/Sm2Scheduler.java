package com.akdemya.domain.service;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified SM-2 scheduler for flashcards with learning steps (Anki-like).
 */
public class Sm2Scheduler {

  private static final double MIN_EASE_FACTOR = 1.3;
  private static final List<Integer> DEFAULT_LEARNING_STEPS = List.of(1, 10);
  private static final int DEFAULT_EASY_INTERVAL_DAYS = 4;
  private static final int DEFAULT_REVIEW_AGAIN_RELEARN_MINUTES = 10;

  private final List<Integer> learningStepsMinutes;
  private final int easyIntervalDays;
  private final int reviewAgainRelearnMinutes;

  public Sm2Scheduler() {
    this(DEFAULT_LEARNING_STEPS, DEFAULT_EASY_INTERVAL_DAYS, DEFAULT_REVIEW_AGAIN_RELEARN_MINUTES);
  }

  public Sm2Scheduler(List<Integer> learningStepsMinutes,
                      int easyIntervalDays,
                      int reviewAgainRelearnMinutes) {
    this.learningStepsMinutes = (learningStepsMinutes == null || learningStepsMinutes.isEmpty())
        ? DEFAULT_LEARNING_STEPS
        : learningStepsMinutes;
    this.easyIntervalDays = easyIntervalDays > 0 ? easyIntervalDays : DEFAULT_EASY_INTERVAL_DAYS;
    this.reviewAgainRelearnMinutes = reviewAgainRelearnMinutes > 0
        ? reviewAgainRelearnMinutes
        : DEFAULT_REVIEW_AGAIN_RELEARN_MINUTES;
  }

  public Result schedule(FlashcardReview review, ReviewGrade grade, LocalDateTime reviewedAt) {
    if (review == null) throw new IllegalArgumentException("review cannot be null");
    if (grade == null) throw new IllegalArgumentException("grade cannot be null");
    if (reviewedAt == null) throw new IllegalArgumentException("reviewedAt cannot be null");

    int intervalBefore = review.getIntervalDays();
    double easeBefore = review.getEaseFactor();
    int learningStepBefore = review.getLearningStep();

    int repetitions = review.getRepetitions();
    int lapses = review.getLapses();
    double easeAfter = easeBefore;
    int intervalAfter = intervalBefore;
    int learningStepAfter = learningStepBefore;
    ReviewState stateAfter;

    if (review.getState() == ReviewState.REVIEW) {
      switch (grade) {
        case AGAIN -> {
          lapses += 1;
          repetitions = 0;
          easeAfter = clampEase(easeBefore - 0.20);
          intervalAfter = 0;
          learningStepAfter = 0;
          stateAfter = ReviewState.LEARNING;
        }
        case HARD -> {
          repetitions += 1;
          easeAfter = clampEase(easeBefore - 0.15);
          intervalAfter = Math.max(1, (int) Math.round(intervalBefore * 1.2));
          learningStepAfter = 0;
          stateAfter = ReviewState.REVIEW;
        }
        case GOOD -> {
          repetitions += 1;
          intervalAfter = computeGoodInterval(repetitions, intervalBefore, easeBefore);
          learningStepAfter = 0;
          stateAfter = ReviewState.REVIEW;
        }
        case EASY -> {
          repetitions += 1;
          easeAfter = clampEase(easeBefore + 0.15);
          intervalAfter = (int) Math.round(intervalBefore * easeBefore * 1.3);
          learningStepAfter = 0;
          stateAfter = ReviewState.REVIEW;
        }
        default -> throw new IllegalStateException("Unexpected grade: " + grade);
      }
    } else {
      int lastStepIndex = learningStepsMinutes.size() - 1;
      switch (grade) {
        case AGAIN -> {
          lapses += 1;
          repetitions = 0;
          easeAfter = clampEase(easeBefore - 0.20);
          intervalAfter = 0;
          learningStepAfter = 0;
          stateAfter = ReviewState.LEARNING;
        }
        case HARD, GOOD -> {
          repetitions += 1;
          if (grade == ReviewGrade.HARD) {
            easeAfter = clampEase(easeBefore - 0.15);
          }
          if (learningStepBefore < lastStepIndex) {
            learningStepAfter = learningStepBefore + 1;
            intervalAfter = 0;
            stateAfter = ReviewState.LEARNING;
          } else {
            learningStepAfter = 0;
            intervalAfter = 1;
            stateAfter = ReviewState.REVIEW;
          }
        }
        case EASY -> {
          repetitions += 1;
          easeAfter = clampEase(easeBefore + 0.15);
          learningStepAfter = 0;
          intervalAfter = easyIntervalDays;
          stateAfter = ReviewState.REVIEW;
        }
        default -> throw new IllegalStateException("Unexpected grade: " + grade);
      }
    }

    LocalDateTime dueAt = computeDueAt(review.getState(), grade, stateAfter, intervalAfter, learningStepAfter, reviewedAt);

    return new Result(
        stateAfter,
        learningStepBefore,
        learningStepAfter,
        repetitions,
        lapses,
        intervalBefore,
        intervalAfter,
        easeBefore,
        easeAfter,
        dueAt
    );
  }

  private LocalDateTime computeDueAt(ReviewState stateBefore,
                                    ReviewGrade grade,
                                    ReviewState stateAfter,
                                    int intervalAfter,
                                    int learningStepAfter,
                                    LocalDateTime reviewedAt) {
    if (stateAfter == ReviewState.LEARNING) {
      if (stateBefore == ReviewState.REVIEW && grade == ReviewGrade.AGAIN) {
        return reviewedAt.plusMinutes(reviewAgainRelearnMinutes);
      }
      int stepMinutes = learningStepsMinutes.get(Math.min(learningStepAfter, learningStepsMinutes.size() - 1));
      return reviewedAt.plusMinutes(stepMinutes);
    }
    return reviewedAt.plusDays(intervalAfter);
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
    private final int learningStepBefore;
    private final int learningStepAfter;
    private final int repetitions;
    private final int lapses;
    private final int intervalBefore;
    private final int intervalAfter;
    private final double easeBefore;
    private final double easeAfter;
    private final LocalDateTime dueAt;

    public Result(ReviewState state,
                  int learningStepBefore,
                  int learningStepAfter,
                  int repetitions,
                  int lapses,
                  int intervalBefore,
                  int intervalAfter,
                  double easeBefore,
                  double easeAfter,
                  LocalDateTime dueAt) {
      this.state = state;
      this.learningStepBefore = learningStepBefore;
      this.learningStepAfter = learningStepAfter;
      this.repetitions = repetitions;
      this.lapses = lapses;
      this.intervalBefore = intervalBefore;
      this.intervalAfter = intervalAfter;
      this.easeBefore = easeBefore;
      this.easeAfter = easeAfter;
      this.dueAt = dueAt;
    }

    public ReviewState getState() { return state; }
    public int getLearningStepBefore() { return learningStepBefore; }
    public int getLearningStepAfter() { return learningStepAfter; }
    public int getRepetitions() { return repetitions; }
    public int getLapses() { return lapses; }
    public int getIntervalBefore() { return intervalBefore; }
    public int getIntervalAfter() { return intervalAfter; }
    public double getEaseBefore() { return easeBefore; }
    public double getEaseAfter() { return easeAfter; }
    public LocalDateTime getDueAt() { return dueAt; }
  }
}
