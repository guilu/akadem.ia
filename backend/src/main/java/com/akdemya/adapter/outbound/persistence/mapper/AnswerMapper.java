package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.AnswerEntity;
import com.akdemya.domain.model.Answer;
import org.springframework.stereotype.Component;

@Component
public class AnswerMapper {
  public Answer toDomain(AnswerEntity entity) {
    return new Answer(entity.getId(), entity.getQuestionId(), entity.getText(), entity.isCorrect());
  }

  public AnswerEntity toEntity(Answer domain) {
    AnswerEntity entity = new AnswerEntity(null, domain.getText(), domain.isCorrect());
    entity.setId(domain.getId());
    return entity;
  }
}
