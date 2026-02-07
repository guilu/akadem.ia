package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import com.akdemya.domain.model.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {
  public Question toDomain(QuestionEntity entity) {
    return new Question(entity.getId(), entity.getUnitId(), entity.getText(), entity.getExplanation(),
        entity.getDifficulty() != null ? Question.Difficulty.valueOf(entity.getDifficulty())
            : Question.Difficulty.MEDIUM);
  }

  public QuestionEntity toEntity(Question domain) {
    QuestionEntity entity = new QuestionEntity(null, domain.getText(), domain.getDifficulty().name());
    entity.setExplanation(domain.getExplanation());
    entity.setId(domain.getId());
    return entity;
  }
}
