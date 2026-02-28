package com.akdemya.adapter.outbound.persistence.entity;

import com.akdemya.domain.model.ReviewState;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "flashcard_reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_flashcard_reviews_user_flashcard",
        columnNames = {"user_id", "flashcard_id"}
    )
)
public class FlashcardReviewEntity {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "flashcard_id", nullable = false)
  private UUID flashcardId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReviewState state;

  @Column(name = "ease_factor", nullable = false)
  private double easeFactor;

  @Column(name = "interval_days", nullable = false)
  private int intervalDays;

  @Column(name = "learning_step", nullable = false)
  private int learningStep;

  @Column(nullable = false)
  private int repetitions;

  @Column(nullable = false)
  private int lapses;

  @Column(name = "due_at", nullable = false)
  private Instant dueAt;

  @Column(name = "last_reviewed_at")
  private Instant lastReviewedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public FlashcardReviewEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public UUID getFlashcardId() { return flashcardId; }
  public void setFlashcardId(UUID flashcardId) { this.flashcardId = flashcardId; }

  public ReviewState getState() { return state; }
  public void setState(ReviewState state) { this.state = state; }

  public double getEaseFactor() { return easeFactor; }
  public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

  public int getIntervalDays() { return intervalDays; }
  public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

  public int getLearningStep() { return learningStep; }
  public void setLearningStep(int learningStep) { this.learningStep = learningStep; }

  public int getRepetitions() { return repetitions; }
  public void setRepetitions(int repetitions) { this.repetitions = repetitions; }

  public int getLapses() { return lapses; }
  public void setLapses(int lapses) { this.lapses = lapses; }

  public Instant getDueAt() { return dueAt; }
  public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

  public Instant getLastReviewedAt() { return lastReviewedAt; }
  public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
