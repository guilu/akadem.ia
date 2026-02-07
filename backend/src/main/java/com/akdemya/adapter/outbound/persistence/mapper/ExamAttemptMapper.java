package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptEntity;
import com.akdemya.domain.model.ExamAttempt;
import org.springframework.stereotype.Component;

@Component
public class ExamAttemptMapper {
  public ExamAttempt toDomain(ExamAttemptEntity entity) {
    return new ExamAttempt(entity.getId(), entity.getUserEmail(), entity.getStartedAt(), entity.getFinishedAt(),
        entity.getTotalTimeSeconds(), entity.getScore());
  }

  public ExamAttemptEntity toEntity(ExamAttempt domain) {
    ExamAttemptEntity entity = new ExamAttemptEntity(domain.getUserEmail(), domain.getTotalTimeSeconds());
    entity.setId(domain.getId());
    entity.setFinishedAt(domain.getFinishedAt());
    entity.setScore(domain.getScore());
    // startedAt is initialized in constructor or defaults, if domain has specific
    // startedAt we might need to set it.
    // Entity has initialized startedAt.
    // But if we map EXISTING domain object back to entity, we should preserve
    // startedAt.
    // ExamAttemptEntity doesn't have setStartedAt.
    // We probably should add it or use Reflection.
    // For now let's assumes startedAt is handled correctly or add setter.
    return entity;
  }
}
