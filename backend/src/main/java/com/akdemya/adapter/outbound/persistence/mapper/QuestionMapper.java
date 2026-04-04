package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {
  public Question toDomain(QuestionEntity entity) {
    Visibility visibility = entity.getVisibility() != null
        ? Visibility.valueOf(entity.getVisibility())
        : Visibility.GLOBAL;
    return new Question(entity.getId(), entity.getUnitId(), entity.getText(), entity.getExplanation(),
        entity.getDifficulty() != null ? Question.Difficulty.valueOf(entity.getDifficulty())
            : Question.Difficulty.MEDIUM,
        visibility, entity.getOwnerId());
  }

  public QuestionEntity toEntity(Question domain) {
    QuestionEntity entity = new QuestionEntity(null, domain.getText(), domain.getDifficulty().name());
    entity.setExplanation(domain.getExplanation());
    entity.setId(domain.getId());
    entity.setVisibility(domain.getVisibility() != null ? domain.getVisibility().name() : Visibility.GLOBAL.name());
    entity.setOwnerId(domain.getOwnerId());
    return entity;
  }
}
