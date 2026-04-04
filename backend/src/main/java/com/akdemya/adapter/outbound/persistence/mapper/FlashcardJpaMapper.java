package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardEntity;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class FlashcardJpaMapper {

  public Flashcard toDomain(FlashcardEntity e) {
    Visibility visibility = e.getVisibility() != null
        ? Visibility.valueOf(e.getVisibility())
        : Visibility.GLOBAL;
    return new Flashcard(
        e.getId(),
        e.getUnitId(),
        e.getFront(),
        e.getBack(),
        toLocalDateTime(e.getCreatedAt()),
        toLocalDateTime(e.getUpdatedAt()),
        visibility,
        e.getOwnerId()
    );
  }

  public FlashcardEntity toEntity(Flashcard d) {
    FlashcardEntity e = new FlashcardEntity();
    e.setId(d.getId());
    e.setUnitId(d.getUnitId());
    e.setFront(d.getFront());
    e.setBack(d.getBack());
    e.setCreatedAt(toInstant(d.getCreatedAt()));
    e.setUpdatedAt(toInstant(d.getUpdatedAt()));
    e.setVisibility(d.getVisibility() != null ? d.getVisibility().name() : Visibility.GLOBAL.name());
    e.setOwnerId(d.getOwnerId());
    return e;
  }

  private static LocalDateTime toLocalDateTime(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(LocalDateTime ldt) {
    return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
  }
}
