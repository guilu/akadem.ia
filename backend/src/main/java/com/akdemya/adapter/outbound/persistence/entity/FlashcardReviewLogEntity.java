package com.akdemya.adapter.outbound.persistence.entity;

import com.akdemya.domain.model.ReviewGrade;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_review_log")
public class FlashcardReviewLogEntity {

  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "flashcard_id", nullable = false)
  private UUID flashcardId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReviewGrade grade;

  @Column(name = "reviewed_at", nullable = false)
  private Instant reviewedAt;

  @Column(name = "interval_before")
  private Integer intervalBefore;

  @Column(name = "interval_after")
  private Integer intervalAfter;

  @Column(name = "ease_before")
  private Double easeBefore;

  @Column(name = "ease_after")
  private Double easeAfter;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public FlashcardReviewLogEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public UUID getFlashcardId() { return flashcardId; }
  public void setFlashcardId(UUID flashcardId) { this.flashcardId = flashcardId; }

  public ReviewGrade getGrade() { return grade; }
  public void setGrade(ReviewGrade grade) { this.grade = grade; }

  public Instant getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

  public Integer getIntervalBefore() { return intervalBefore; }
  public void setIntervalBefore(Integer intervalBefore) { this.intervalBefore = intervalBefore; }

  public Integer getIntervalAfter() { return intervalAfter; }
  public void setIntervalAfter(Integer intervalAfter) { this.intervalAfter = intervalAfter; }

  public Double getEaseBefore() { return easeBefore; }
  public void setEaseBefore(Double easeBefore) { this.easeBefore = easeBefore; }

  public Double getEaseAfter() { return easeAfter; }
  public void setEaseAfter(Double easeAfter) { this.easeAfter = easeAfter; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
