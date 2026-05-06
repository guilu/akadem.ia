package com.akdemya.adapter.outbound.persistence.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnswerEntityTest {

  @Test
  void defaultConstructor_assignsRandomId() {
    AnswerEntity entity = new AnswerEntity();
    assertNotNull(entity.getId());
  }

  @Test
  void defaultCorrect_isFalse() {
    AnswerEntity entity = new AnswerEntity();
    assertFalse(entity.isCorrect());
  }

  @Test
  void parameterizedConstructor_setsFields() {
    QuestionEntity question = new QuestionEntity();
    question.setId(UUID.randomUUID());
    AnswerEntity entity = new AnswerEntity(question, "Some answer", true);

    assertEquals(question, entity.getQuestion());
    assertEquals("Some answer", entity.getText());
    assertTrue(entity.isCorrect());
  }

  @Test
  void getQuestionId_returnsQuestionIdWhenSet() {
    QuestionEntity question = new QuestionEntity();
    UUID qId = UUID.randomUUID();
    question.setId(qId);

    AnswerEntity entity = new AnswerEntity();
    entity.setQuestion(question);

    assertEquals(qId, entity.getQuestionId());
  }

  @Test
  void getQuestionId_returnsNullWhenQuestionIsNull() {
    AnswerEntity entity = new AnswerEntity();
    assertNull(entity.getQuestionId());
  }

  @Test
  void setters_updateAllFields() {
    AnswerEntity entity = new AnswerEntity();
    UUID id = UUID.randomUUID();
    QuestionEntity question = new QuestionEntity();

    entity.setId(id);
    entity.setQuestion(question);
    entity.setText("Updated answer");
    entity.setCorrect(true);

    assertEquals(id, entity.getId());
    assertEquals(question, entity.getQuestion());
    assertEquals("Updated answer", entity.getText());
    assertTrue(entity.isCorrect());
  }
}
