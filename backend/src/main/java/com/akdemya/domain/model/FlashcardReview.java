package com.akdemya.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SRS state for a given user + flashcard pair.
 * Mutable fields reflect the spaced-repetition algorithm output after each review.
 */
public class FlashcardReview {

  private static final double MIN_EASE_FACTOR = 1.3;

  private final UUID id;
  private final UUID userId;
  private final UUID flashcardId;
  private ReviewState state;
  private double easeFactor;
  private int intervalDays;
  private int learningStep;
  private int repetitions;
  private int lapses;
  private LocalDateTime dueAt;
  private LocalDateTime lastReviewedAt;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public FlashcardReview(UUID id, UUID userId, UUID flashcardId,
                         ReviewState state, double easeFactor, int intervalDays,
                         int learningStep, int repetitions, int lapses,
                         LocalDateTime dueAt, LocalDateTime lastReviewedAt,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
    if (id == null) throw new IllegalArgumentException("id cannot be null");
    if (userId == null) throw new IllegalArgumentException("userId cannot be null");
    if (flashcardId == null) throw new IllegalArgumentException("flashcardId cannot be null");
    if (state == null) throw new IllegalArgumentException("state cannot be null");
    if (easeFactor < MIN_EASE_FACTOR)
      throw new IllegalArgumentException("easeFactor must be >= " + MIN_EASE_FACTOR);
    if (intervalDays < 0) throw new IllegalArgumentException("intervalDays cannot be negative");
    if (learningStep < 0) throw new IllegalArgumentException("learningStep cannot be negative");
    if (repetitions < 0) throw new IllegalArgumentException("repetitions cannot be negative");
    if (lapses < 0) throw new IllegalArgumentException("lapses cannot be negative");
    if (dueAt == null) throw new IllegalArgumentException("dueAt cannot be null");
    if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
    if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");
    this.id = id;
    this.userId = userId;
    this.flashcardId = flashcardId;
    this.state = state;
    this.easeFactor = easeFactor;
    this.intervalDays = intervalDays;
    this.learningStep = learningStep;
    this.repetitions = repetitions;
    this.lapses = lapses;
    this.dueAt = dueAt;
    this.lastReviewedAt = lastReviewedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** Factory for the first time a user encounters a flashcard. */
  public static FlashcardReview createNew(UUID userId, UUID flashcardId, LocalDateTime dueAt) {
    LocalDateTime now = LocalDateTime.now();
    return new FlashcardReview(
        UUID.randomUUID(), userId, flashcardId,
        ReviewState.NEW, 2.5, 0, 0, 0, 0,
        dueAt, null, now, now);
  }

  /** Apply the result of an SM-2 calculation (called from the use-case layer). */
  public void update(ReviewState state, double easeFactor, int intervalDays,
                     int learningStep, int repetitions, int lapses, LocalDateTime dueAt) {
    if (state == null) throw new IllegalArgumentException("state cannot be null");
    if (easeFactor < MIN_EASE_FACTOR)
      throw new IllegalArgumentException("easeFactor must be >= " + MIN_EASE_FACTOR);
    if (intervalDays < 0) throw new IllegalArgumentException("intervalDays cannot be negative");
    if (learningStep < 0) throw new IllegalArgumentException("learningStep cannot be negative");
    if (repetitions < 0) throw new IllegalArgumentException("repetitions cannot be negative");
    if (lapses < 0) throw new IllegalArgumentException("lapses cannot be negative");
    if (dueAt == null) throw new IllegalArgumentException("dueAt cannot be null");
    this.state = state;
    this.easeFactor = easeFactor;
    this.intervalDays = intervalDays;
    this.learningStep = learningStep;
    this.repetitions = repetitions;
    this.lapses = lapses;
    this.dueAt = dueAt;
    this.lastReviewedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public UUID getFlashcardId() { return flashcardId; }
  public ReviewState getState() { return state; }
  public double getEaseFactor() { return easeFactor; }
  public int getIntervalDays() { return intervalDays; }
  public int getLearningStep() { return learningStep; }
  public int getRepetitions() { return repetitions; }
  public int getLapses() { return lapses; }
  public LocalDateTime getDueAt() { return dueAt; }
  public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
