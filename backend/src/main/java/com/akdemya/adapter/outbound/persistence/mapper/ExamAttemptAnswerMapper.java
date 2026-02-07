package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptAnswerEntity;
import com.akdemya.domain.model.ExamAttemptAnswer;
import org.springframework.stereotype.Component;

@Component
public class ExamAttemptAnswerMapper {
  public ExamAttemptAnswer toDomain(ExamAttemptAnswerEntity entity) {
    return new ExamAttemptAnswer(entity.getId(), entity.getAttemptId(), entity.getQuestionId(),
        entity.getSelectedAnswerId());
  }

  public ExamAttemptAnswerEntity toEntity(ExamAttemptAnswer domain) {
    ExamAttemptAnswerEntity entity = new ExamAttemptAnswerEntity(domain.getExamAttemptId(), domain.getQuestionId());
    entity.setId(domain.getId());
    entity.setSelectedAnswerId(domain.getAnswerId());
    return entity;
  }
}
