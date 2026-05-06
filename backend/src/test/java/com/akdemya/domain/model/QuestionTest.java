package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

  @Test
  void createGlobalQuestionHasGlobalVisibilityAndNullOwner() {
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "What is 2+2?", "Basic addition", Question.Difficulty.EASY);
    assertEquals(Visibility.GLOBAL, q.getVisibility());
    assertNull(q.getOwnerId());
    assertNotNull(q.getId());
    assertEquals(unitId, q.getUnitId());
    assertEquals("What is 2+2?", q.getText());
    assertEquals("Basic addition", q.getExplanation());
    assertEquals(Question.Difficulty.EASY, q.getDifficulty());
  }

  @Test
  void createPrivateQuestionHasPrivateVisibilityAndOwner() {
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Question q = Question.createPrivate(unitId, "My question?", "exp", Question.Difficulty.HARD, ownerId);
    assertEquals(Visibility.PRIVATE, q.getVisibility());
    assertEquals(ownerId, q.getOwnerId());
    assertNotNull(q.getId());
  }

  @Test
  void createPrivateQuestionWithNullOwnerIdThrows() {
    UUID unitId = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class,
        () -> Question.createPrivate(unitId, "Q?", "exp", Question.Difficulty.MEDIUM, null));
  }

  @Test
  void createFactoryDefaultsToGlobal() {
    UUID unitId = UUID.randomUUID();
    Question q = Question.create(unitId, "Q?", "exp", Question.Difficulty.MEDIUM);
    assertEquals(Visibility.GLOBAL, q.getVisibility());
    assertNull(q.getOwnerId());
  }

  @Test
  void backwardCompatibleConstructorDefaultsToGlobal() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Question q = new Question(id, unitId, "Q?", "exp", Question.Difficulty.EASY);
    assertEquals(Visibility.GLOBAL, q.getVisibility());
    assertNull(q.getOwnerId());
    assertEquals(id, q.getId());
    assertEquals(unitId, q.getUnitId());
    assertEquals("Q?", q.getText());
    assertEquals("exp", q.getExplanation());
    assertEquals(Question.Difficulty.EASY, q.getDifficulty());
  }

  @Test
  void fullConstructorSetsAllFields() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Question q = new Question(id, unitId, "Hard Q?", "complex exp", Question.Difficulty.HARD,
        Visibility.PRIVATE, ownerId);
    assertEquals(id, q.getId());
    assertEquals(unitId, q.getUnitId());
    assertEquals("Hard Q?", q.getText());
    assertEquals("complex exp", q.getExplanation());
    assertEquals(Question.Difficulty.HARD, q.getDifficulty());
    assertEquals(Visibility.PRIVATE, q.getVisibility());
    assertEquals(ownerId, q.getOwnerId());
  }

  @Test
  void difficultyEnumValuesExist() {
    assertNotNull(Question.Difficulty.valueOf("EASY"));
    assertNotNull(Question.Difficulty.valueOf("MEDIUM"));
    assertNotNull(Question.Difficulty.valueOf("HARD"));
  }
}
