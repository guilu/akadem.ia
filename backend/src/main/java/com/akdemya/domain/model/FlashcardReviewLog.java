package com.akdemya.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable historical record of a single flashcard review event.
 */
public class FlashcardReviewLog {

  private final UUID id;
  private final UUID userId;
  private final UUID flashcardId;
  private final ReviewGrade grade;
  private final LocalDateTime reviewedAt;
  private final Integer intervalBefore;
  private final Integer intervalAfter;
  private final Double easeBefore;
  private final Double easeAfter;
  private final LocalDateTime createdAt;

  public FlashcardReviewLog(UUID id, UUID userId, UUID flashcardId, ReviewGrade grade,
                            LocalDateTime reviewedAt,
                            Integer intervalBefore, Integer intervalAfter,
                            Double easeBefore, Double easeAfter,
                            LocalDateTime createdAt) {
    if (id == null) throw new IllegalArgumentException("id cannot be null");
    if (userId == null) throw new IllegalArgumentException("userId cannot be null");
    if (flashcardId == null) throw new IllegalArgumentException("flashcardId cannot be null");
    if (grade == null) throw new IllegalArgumentException("grade cannot be null");
    if (reviewedAt == null) throw new IllegalArgumentException("reviewedAt cannot be null");
    if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
    this.id = id;
    this.userId = userId;
    this.flashcardId = flashcardId;
    this.grade = grade;
    this.reviewedAt = reviewedAt;
    this.intervalBefore = intervalBefore;
    this.intervalAfter = intervalAfter;
    this.easeBefore = easeBefore;
    this.easeAfter = easeAfter;
    this.createdAt = createdAt;
  }

  public static FlashcardReviewLog create(UUID userId, UUID flashcardId, ReviewGrade grade,
                                          LocalDateTime reviewedAt,
                                          Integer intervalBefore, Integer intervalAfter,
                                          Double easeBefore, Double easeAfter) {
    return new FlashcardReviewLog(
        UUID.randomUUID(), userId, flashcardId, grade, reviewedAt,
        intervalBefore, intervalAfter, easeBefore, easeAfter,
        LocalDateTime.now());
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public UUID getFlashcardId() { return flashcardId; }
  public ReviewGrade getGrade() { return grade; }
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  public Integer getIntervalBefore() { return intervalBefore; }
  public Integer getIntervalAfter() { return intervalAfter; }
  public Double getEaseBefore() { return easeBefore; }
  public Double getEaseAfter() { return easeAfter; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
