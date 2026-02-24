package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewLogEntity;
import com.akdemya.domain.model.FlashcardReviewLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class FlashcardReviewLogJpaMapper {

  public FlashcardReviewLog toDomain(FlashcardReviewLogEntity e) {
    return new FlashcardReviewLog(
        e.getId(),
        e.getUserId(),
        e.getFlashcardId(),
        e.getGrade(),
        toLocalDateTime(e.getReviewedAt()),
        e.getIntervalBefore(),
        e.getIntervalAfter(),
        e.getEaseBefore(),
        e.getEaseAfter(),
        toLocalDateTime(e.getCreatedAt())
    );
  }

  public FlashcardReviewLogEntity toEntity(FlashcardReviewLog d) {
    FlashcardReviewLogEntity e = new FlashcardReviewLogEntity();
    e.setId(d.getId());
    e.setUserId(d.getUserId());
    e.setFlashcardId(d.getFlashcardId());
    e.setGrade(d.getGrade());
    e.setReviewedAt(toInstant(d.getReviewedAt()));
    e.setIntervalBefore(d.getIntervalBefore());
    e.setIntervalAfter(d.getIntervalAfter());
    e.setEaseBefore(d.getEaseBefore());
    e.setEaseAfter(d.getEaseAfter());
    e.setCreatedAt(toInstant(d.getCreatedAt()));
    return e;
  }

  private static LocalDateTime toLocalDateTime(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(LocalDateTime ldt) {
    return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
  }
}
