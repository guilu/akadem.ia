package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.domain.model.FlashcardReview;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class FlashcardReviewJpaMapper {

  public FlashcardReview toDomain(FlashcardReviewEntity e) {
    return new FlashcardReview(
        e.getId(),
        e.getUserId(),
        e.getFlashcardId(),
        e.getState(),
        e.getEaseFactor(),
        e.getIntervalDays(),
        e.getRepetitions(),
        e.getLapses(),
        toLocalDateTime(e.getDueAt()),
        toLocalDateTime(e.getLastReviewedAt()),
        toLocalDateTime(e.getCreatedAt()),
        toLocalDateTime(e.getUpdatedAt())
    );
  }

  public FlashcardReviewEntity toEntity(FlashcardReview d) {
    FlashcardReviewEntity e = new FlashcardReviewEntity();
    e.setId(d.getId());
    e.setUserId(d.getUserId());
    e.setFlashcardId(d.getFlashcardId());
    e.setState(d.getState());
    e.setEaseFactor(d.getEaseFactor());
    e.setIntervalDays(d.getIntervalDays());
    e.setRepetitions(d.getRepetitions());
    e.setLapses(d.getLapses());
    e.setDueAt(toInstant(d.getDueAt()));
    e.setLastReviewedAt(toInstant(d.getLastReviewedAt()));
    e.setCreatedAt(toInstant(d.getCreatedAt()));
    e.setUpdatedAt(toInstant(d.getUpdatedAt()));
    return e;
  }

  /** Patch the mutable SRS fields on an existing entity (used in upsert logic). */
  public void updateEntity(FlashcardReviewEntity e, FlashcardReview d) {
    e.setState(d.getState());
    e.setEaseFactor(d.getEaseFactor());
    e.setIntervalDays(d.getIntervalDays());
    e.setRepetitions(d.getRepetitions());
    e.setLapses(d.getLapses());
    e.setDueAt(toInstant(d.getDueAt()));
    e.setLastReviewedAt(toInstant(d.getLastReviewedAt()));
    e.setUpdatedAt(toInstant(d.getUpdatedAt()));
  }

  private static LocalDateTime toLocalDateTime(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(LocalDateTime ldt) {
    return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
  }
}
