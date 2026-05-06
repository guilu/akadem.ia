package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentManagementServiceTest {

  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final QuestionRepository questionRepo = mock(QuestionRepository.class);
  private final AnswerRepository answerRepo = mock(AnswerRepository.class);

  private final com.akdemya.domain.port.out.SyllabusRepository syllabusRepo = mock(com.akdemya.domain.port.out.SyllabusRepository.class);
  private final ContentManagement service =
      new ContentManagement(subjectRepo, unitRepo, questionRepo, answerRepo, syllabusRepo);

  // --- Subject ---

  @Test
  void deleteSubjectDelegatesToRepo() {
    UUID id = UUID.randomUUID();
    service.deleteSubject(id);
    verify(subjectRepo).deleteById(id);
  }

  // --- Unit ---

  @Test
  void getUnitsBySubjectDelegatesToRepo() {
    UUID subjectId = UUID.randomUUID();
    Unit unit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(unitRepo.findBySubjectId(subjectId)).thenReturn(List.of(unit));

    List<Unit> result = service.getUnitsBySubject(subjectId);

    assertEquals(1, result.size());
    verify(unitRepo).findBySubjectId(subjectId);
  }

  @Test
  void createUnitSavesAndReturns() {
    UUID subjectId = UUID.randomUUID();
    Subject globalSubject = Subject.createGlobal("Math", "desc");
    // Use the subject's own ID so the unit's subjectId matches
    Subject parentSubject = new Subject(subjectId, "Math", "desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(subjectId)).thenReturn(java.util.Optional.of(parentSubject));
    Unit unit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(unitRepo.save(unit)).thenReturn(unit);

    Unit saved = service.createUnit(unit);

    assertEquals(unit, saved);
    verify(unitRepo).save(unit);
  }

  @Test
  void deleteUnitDelegatesToRepo() {
    UUID id = UUID.randomUUID();
    service.deleteUnit(id);
    verify(unitRepo).deleteById(id);
  }

  @Test
  void getUnitAvailabilityWithNoDifficultyCountsAll() {
    UUID subjectId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Unit unit = new Unit(unitId, subjectId, "Calculus", "desc", 1);
    when(unitRepo.findBySubjectId(subjectId)).thenReturn(List.of(unit));
    when(questionRepo.countByUnitId(unitId)).thenReturn(5L);

    List<ContentManagement.UnitAvailability> result = service.getUnitAvailability(subjectId);

    assertEquals(1, result.size());
    assertEquals(5L, result.get(0).available());
    assertEquals(unitId, result.get(0).id());
    assertEquals("Calculus", result.get(0).name());
  }

  @Test
  void getUnitAvailabilityWithDifficultyCountsByDifficulty() {
    UUID subjectId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Unit unit = new Unit(unitId, subjectId, "Geometry", "desc", 2);
    when(unitRepo.findBySubjectId(subjectId)).thenReturn(List.of(unit));
    when(questionRepo.countByUnitIdAndDifficulty(unitId, "EASY")).thenReturn(3L);

    List<ContentManagement.UnitAvailability> result =
        service.getUnitAvailability(subjectId, Question.Difficulty.EASY);

    assertEquals(1, result.size());
    assertEquals(3L, result.get(0).available());
    verify(questionRepo).countByUnitIdAndDifficulty(unitId, "EASY");
    verify(questionRepo, never()).countByUnitId(any());
  }

  // --- Question ---

  @Test
  void getQuestionsByUnitDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.MEDIUM);
    when(questionRepo.findByUnitId(unitId)).thenReturn(List.of(q));

    List<Question> result = service.getQuestionsByUnit(unitId);

    assertEquals(1, result.size());
    verify(questionRepo).findByUnitId(unitId);
  }

  @Test
  void createQuestionSavesAndReturns() {
    UUID unitId = UUID.randomUUID();
    Unit parentUnit = Unit.createGlobal(UUID.randomUUID(), "Algebra", "desc", 1);
    // Create a parent unit with the same unitId
    Unit parentUnitWithId = new Unit(unitId, UUID.randomUUID(), "Algebra", "desc", 1, Visibility.GLOBAL, null);
    when(unitRepo.findById(unitId)).thenReturn(java.util.Optional.of(parentUnitWithId));
    Question q = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.HARD);
    when(questionRepo.save(q)).thenReturn(q);

    Question saved = service.createQuestion(q);

    assertEquals(q, saved);
    verify(questionRepo).save(q);
  }

  @Test
  void deleteQuestionDelegatesToRepo() {
    UUID id = UUID.randomUUID();
    service.deleteQuestion(id);
    verify(questionRepo).deleteById(id);
  }

  // --- Answer ---

  @Test
  void getAnswersByQuestionDelegatesToRepo() {
    UUID questionId = UUID.randomUUID();
    Answer a = Answer.create(questionId, "Option A", true);
    when(answerRepo.findByQuestionId(questionId)).thenReturn(List.of(a));

    List<Answer> result = service.getAnswersByQuestion(questionId);

    assertEquals(1, result.size());
    verify(answerRepo).findByQuestionId(questionId);
  }

  @Test
  void createAnswerSavesWhenFewerthanFour() {
    UUID questionId = UUID.randomUUID();
    Answer a = Answer.create(questionId, "Option A", true);
    when(answerRepo.findByQuestionId(questionId)).thenReturn(List.of());
    when(answerRepo.save(a)).thenReturn(a);

    Answer saved = service.createAnswer(a);

    assertEquals(a, saved);
    verify(answerRepo).save(a);
  }

  @Test
  void createAnswerThrowsWhenAlreadyFourAnswers() {
    UUID questionId = UUID.randomUUID();
    Answer a = Answer.create(questionId, "Option E", false);
    List<Answer> existing = List.of(
        Answer.create(questionId, "A", true),
        Answer.create(questionId, "B", false),
        Answer.create(questionId, "C", false),
        Answer.create(questionId, "D", false)
    );
    when(answerRepo.findByQuestionId(questionId)).thenReturn(existing);

    assertThrows(IllegalArgumentException.class, () -> service.createAnswer(a));
    verify(answerRepo, never()).save(any());
  }

  @Test
  void deleteAnswerDelegatesToRepo() {
    UUID id = UUID.randomUUID();
    service.deleteAnswer(id);
    verify(answerRepo).deleteById(id);
  }

  @Test
  void deleteAnswersByQuestionDelegatesToRepo() {
    UUID questionId = UUID.randomUUID();
    service.deleteAnswersByQuestion(questionId);
    verify(answerRepo).deleteByQuestionId(questionId);
  }
}
