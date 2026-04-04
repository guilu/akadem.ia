package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestionMapperTest {

  private final QuestionMapper mapper = new QuestionMapper();

  @Test
  void toDomainMapsAllFieldsWithExplicitVisibility() {
    QuestionEntity entity = new QuestionEntity(null, "What is 2+2?", "EASY");
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    entity.setId(id);
    entity.setExplanation("Basic addition");
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);

    Question domain = mapper.toDomain(entity);

    assertEquals(id, domain.getId());
    assertEquals("What is 2+2?", domain.getText());
    assertEquals("Basic addition", domain.getExplanation());
    assertEquals(Question.Difficulty.EASY, domain.getDifficulty());
    assertEquals(Visibility.PRIVATE, domain.getVisibility());
    assertEquals(ownerId, domain.getOwnerId());
  }

  @Test
  void toDomainDefaultsVisibilityToGlobalWhenNull() {
    QuestionEntity entity = new QuestionEntity(null, "Q?", "HARD");
    entity.setVisibility(null);

    Question domain = mapper.toDomain(entity);

    assertEquals(Visibility.GLOBAL, domain.getVisibility());
  }

  @Test
  void toDomainDefaultsDifficultyToMediumWhenNull() {
    QuestionEntity entity = new QuestionEntity(null, "Q?", null);

    Question domain = mapper.toDomain(entity);

    assertEquals(Question.Difficulty.MEDIUM, domain.getDifficulty());
  }

  @Test
  void toEntityMapsAllFieldsWithExplicitVisibility() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Question domain = new Question(id, unitId, "Q?", "exp", Question.Difficulty.HARD, Visibility.PRIVATE, ownerId);

    QuestionEntity entity = mapper.toEntity(domain);

    assertEquals(id, entity.getId());
    assertEquals("Q?", entity.getText());
    assertEquals("exp", entity.getExplanation());
    assertEquals("HARD", entity.getDifficulty());
    assertEquals("PRIVATE", entity.getVisibility());
    assertEquals(ownerId, entity.getOwnerId());
  }

  @Test
  void toEntityDefaultsVisibilityToGlobalWhenDomainVisibilityIsNull() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Question domain = new Question(id, unitId, "Q?", "exp", Question.Difficulty.EASY, null, null);

    QuestionEntity entity = mapper.toEntity(domain);

    assertEquals("GLOBAL", entity.getVisibility());
    assertNull(entity.getOwnerId());
  }
}
