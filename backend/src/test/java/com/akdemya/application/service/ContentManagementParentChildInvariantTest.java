package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.out.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for parent-child scope/ownership invariants in ContentManagement.
 * These ensure the domain rules are enforced at the service layer before any persistence.
 */
class ContentManagementParentChildInvariantTest {

  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final QuestionRepository questionRepo = mock(QuestionRepository.class);
  private final AnswerRepository answerRepo = mock(AnswerRepository.class);

  private final ContentManagement service =
      new ContentManagement(subjectRepo, unitRepo, questionRepo, answerRepo);

  // === Unit under Subject invariants ===

  @Test
  void createGlobalUnitUnderGlobalSubjectSucceeds() {
    UUID subjectId = UUID.randomUUID();
    Subject globalSubject = new Subject(subjectId, "Math", "desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(globalSubject));

    Unit globalUnit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(unitRepo.save(globalUnit)).thenReturn(globalUnit);

    Unit saved = service.createUnit(globalUnit);

    assertNotNull(saved);
    verify(unitRepo).save(globalUnit);
  }

  @Test
  void createPrivateUnitUnderPrivateSubjectWithSameOwnerSucceeds() {
    UUID ownerId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Subject privateSubject = new Subject(subjectId, "My Math", "desc", Visibility.PRIVATE, ownerId);
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(privateSubject));

    Unit privateUnit = Unit.createPrivate(subjectId, "My Algebra", "desc", 1, ownerId);
    when(unitRepo.save(privateUnit)).thenReturn(privateUnit);

    Unit saved = service.createUnit(privateUnit);

    assertNotNull(saved);
    verify(unitRepo).save(privateUnit);
  }

  @Test
  void createPrivateUnitUnderGlobalSubjectThrows() {
    UUID subjectId = UUID.randomUUID();
    Subject globalSubject = new Subject(subjectId, "Math", "desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(globalSubject));

    UUID ownerId = UUID.randomUUID();
    Unit privateUnit = Unit.createPrivate(subjectId, "My Algebra", "desc", 1, ownerId);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createUnit(privateUnit));
    assertTrue(ex.getMessage().contains("PRIVATE unit") && ex.getMessage().contains("GLOBAL subject"),
        "Expected message about PRIVATE unit under GLOBAL subject, got: " + ex.getMessage());
    verify(unitRepo, never()).save(any());
  }

  @Test
  void createGlobalUnitUnderPrivateSubjectThrows() {
    UUID ownerId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Subject privateSubject = new Subject(subjectId, "My Math", "desc", Visibility.PRIVATE, ownerId);
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(privateSubject));

    Unit globalUnit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createUnit(globalUnit));
    assertTrue(ex.getMessage().contains("GLOBAL unit") && ex.getMessage().contains("PRIVATE subject"),
        "Expected message about GLOBAL unit under PRIVATE subject, got: " + ex.getMessage());
    verify(unitRepo, never()).save(any());
  }

  @Test
  void createPrivateUnitUnderPrivateSubjectWithDifferentOwnerThrows() {
    UUID ownerA = UUID.randomUUID();
    UUID ownerB = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Subject privateSubject = new Subject(subjectId, "A's Math", "desc", Visibility.PRIVATE, ownerA);
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(privateSubject));

    Unit privateUnit = Unit.createPrivate(subjectId, "B's Algebra", "desc", 1, ownerB);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createUnit(privateUnit));
    assertTrue(ex.getMessage().contains("same user"),
        "Expected message about same user ownership, got: " + ex.getMessage());
    verify(unitRepo, never()).save(any());
  }

  @Test
  void createUnitWithNonExistentSubjectThrows() {
    UUID subjectId = UUID.randomUUID();
    when(subjectRepo.findById(subjectId)).thenReturn(Optional.empty());

    Unit unit = Unit.createGlobal(subjectId, "Algebra", "desc", 1);

    assertThrows(IllegalArgumentException.class, () -> service.createUnit(unit));
    verify(unitRepo, never()).save(any());
  }

  // === Question under Unit invariants ===

  @Test
  void createGlobalQuestionUnderGlobalUnitSucceeds() {
    UUID unitId = UUID.randomUUID();
    Unit globalUnit = new Unit(unitId, UUID.randomUUID(), "Algebra", "desc", 1, Visibility.GLOBAL, null);
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(globalUnit));

    Question globalQ = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.EASY);
    when(questionRepo.save(globalQ)).thenReturn(globalQ);

    Question saved = service.createQuestion(globalQ);

    assertNotNull(saved);
    verify(questionRepo).save(globalQ);
  }

  @Test
  void createPrivateQuestionUnderPrivateUnitWithSameOwnerSucceeds() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Unit privateUnit = new Unit(unitId, UUID.randomUUID(), "My Algebra", "desc", 1, Visibility.PRIVATE, ownerId);
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(privateUnit));

    Question privateQ = Question.createPrivate(unitId, "My Q?", "exp", Question.Difficulty.EASY, ownerId);
    when(questionRepo.save(privateQ)).thenReturn(privateQ);

    Question saved = service.createQuestion(privateQ);

    assertNotNull(saved);
    verify(questionRepo).save(privateQ);
  }

  @Test
  void createPrivateQuestionUnderGlobalUnitThrows() {
    UUID unitId = UUID.randomUUID();
    Unit globalUnit = new Unit(unitId, UUID.randomUUID(), "Algebra", "desc", 1, Visibility.GLOBAL, null);
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(globalUnit));

    UUID ownerId = UUID.randomUUID();
    Question privateQ = Question.createPrivate(unitId, "My Q?", "exp", Question.Difficulty.EASY, ownerId);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createQuestion(privateQ));
    assertTrue(ex.getMessage().contains("PRIVATE question") && ex.getMessage().contains("GLOBAL unit"),
        "Expected message about PRIVATE question under GLOBAL unit, got: " + ex.getMessage());
    verify(questionRepo, never()).save(any());
  }

  @Test
  void createGlobalQuestionUnderPrivateUnitThrows() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Unit privateUnit = new Unit(unitId, UUID.randomUUID(), "My Algebra", "desc", 1, Visibility.PRIVATE, ownerId);
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(privateUnit));

    Question globalQ = Question.createGlobal(unitId, "Global Q?", "exp", Question.Difficulty.MEDIUM);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createQuestion(globalQ));
    assertTrue(ex.getMessage().contains("GLOBAL question") && ex.getMessage().contains("PRIVATE unit"),
        "Expected message about GLOBAL question under PRIVATE unit, got: " + ex.getMessage());
    verify(questionRepo, never()).save(any());
  }

  @Test
  void createPrivateQuestionUnderPrivateUnitWithDifferentOwnerThrows() {
    UUID ownerA = UUID.randomUUID();
    UUID ownerB = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Unit privateUnit = new Unit(unitId, UUID.randomUUID(), "A's Algebra", "desc", 1, Visibility.PRIVATE, ownerA);
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(privateUnit));

    Question privateQ = Question.createPrivate(unitId, "B's Q?", "exp", Question.Difficulty.EASY, ownerB);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.createQuestion(privateQ));
    assertTrue(ex.getMessage().contains("same user"),
        "Expected message about same user ownership, got: " + ex.getMessage());
    verify(questionRepo, never()).save(any());
  }

  @Test
  void createQuestionWithNonExistentUnitThrows() {
    UUID unitId = UUID.randomUUID();
    when(unitRepo.findById(unitId)).thenReturn(Optional.empty());

    Question q = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.EASY);

    assertThrows(IllegalArgumentException.class, () -> service.createQuestion(q));
    verify(questionRepo, never()).save(any());
  }
}
