package com.akdemya.adapter.inbound.web.dto;

import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FlashcardDto {

  public record FlashcardResponse(UUID id, UUID unitId, String front, String back,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
  }

  public record CreateRequest(UUID unitId, String front, String back) {
  }

  public record UpdateRequest(UUID unitId, String front, String back) {
  }

  public record StudyQueueItem(UUID flashcardId, String front, String back,
                               ReviewState state, LocalDateTime dueAt) {
  }

  public record StudyQueueResponse(List<StudyQueueItem> items) {
  }

  public record IntervalHints(String again, String good, String easy) {
  }

  public record StudyNextResponse(UUID flashcardId, UUID unitId, String front, String back,
                                  ReviewState state, LocalDateTime dueAt,
                                  IntervalHints intervalHints) {
  }

  public record ReviewRequest(UUID flashcardId, ReviewGrade grade, LocalDateTime reviewedAt) {
  }

  public record Review(UUID id, UUID userId, UUID flashcardId, ReviewState state,
                       double easeFactor, int intervalDays, int learningStep,
                       int repetitions, int lapses,
                       LocalDateTime dueAt, LocalDateTime lastReviewedAt) {
  }

  public record ReviewLog(UUID id, UUID flashcardId, ReviewGrade grade,
                          LocalDateTime reviewedAt, Integer intervalBefore,
                          Integer intervalAfter, Double easeBefore, Double easeAfter) {
  }

  public record ReviewResponse(Review review, ReviewLog log) {
  }

  public record DashboardResponse(long dueToday,
                                  long dueIn1to3Days,
                                  long dueIn4to7Days,
                                  long dueIn8to30Days,
                                  long newCards,
                                  long totalDue) {
  }

  public record UnitSummary(UUID unitId, String unitName, long newCount, long reviewCount, long dueCount) {
  }

  public record HistoryItem(UUID id, UUID flashcardId, String front, String back,
                            ReviewGrade grade, LocalDateTime reviewedAt,
                            Integer intervalBefore, Integer intervalAfter,
                            Double easeBefore, Double easeAfter) {
  }
}
