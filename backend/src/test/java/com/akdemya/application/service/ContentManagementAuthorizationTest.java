package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.out.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for deleteIfAuthorized and scope-query delegates in ContentManagement.
 * Covers the branch gaps identified by JaCoCo.
 */
class ContentManagementAuthorizationTest {

  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final QuestionRepository questionRepo = mock(QuestionRepository.class);
  private final AnswerRepository answerRepo = mock(AnswerRepository.class);

  private final ContentManagement service =
      new ContentManagement(subjectRepo, unitRepo, questionRepo, answerRepo);

  // ===================== Subject =====================

  @Test
  void deleteSubjectIfAuthorized_adminDeletesGlobalSubject() {
    UUID id = UUID.randomUUID();
    Subject global = new Subject(id, "Math", "desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(id)).thenReturn(Optional.of(global));

    assertDoesNotThrow(() -> service.deleteSubjectIfAuthorized(id, UUID.randomUUID(), true));
    verify(subjectRepo).deleteById(id);
  }

  @Test
  void deleteSubjectIfAuthorized_nonAdminCannotDeleteGlobalSubject() {
    UUID id = UUID.randomUUID();
    Subject global = new Subject(id, "Math", "desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(id)).thenReturn(Optional.of(global));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteSubjectIfAuthorized(id, UUID.randomUUID(), false));
    verify(subjectRepo, never()).deleteById(any());
  }

  @Test
  void deleteSubjectIfAuthorized_ownerDeletesPrivateSubject() {
    UUID ownerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Subject priv = new Subject(id, "My Math", "desc", Visibility.PRIVATE, ownerId);
    when(subjectRepo.findById(id)).thenReturn(Optional.of(priv));

    assertDoesNotThrow(() -> service.deleteSubjectIfAuthorized(id, ownerId, false));
    verify(subjectRepo).deleteById(id);
  }

  @Test
  void deleteSubjectIfAuthorized_nonOwnerCannotDeletePrivateSubject() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Subject priv = new Subject(id, "My Math", "desc", Visibility.PRIVATE, ownerId);
    when(subjectRepo.findById(id)).thenReturn(Optional.of(priv));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteSubjectIfAuthorized(id, callerId, false));
    verify(subjectRepo, never()).deleteById(any());
  }

  @Test
  void deleteSubjectIfAuthorized_throwsWhenSubjectNotFound() {
    UUID id = UUID.randomUUID();
    when(subjectRepo.findById(id)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteSubjectIfAuthorized(id, UUID.randomUUID(), true));
  }

  // ===================== Unit =====================

  @Test
  void deleteUnitIfAuthorized_adminDeletesGlobalUnit() {
    UUID id = UUID.randomUUID();
    Unit global = new Unit(id, UUID.randomUUID(), "Algebra", "desc", 1, Visibility.GLOBAL, null);
    when(unitRepo.findById(id)).thenReturn(Optional.of(global));

    assertDoesNotThrow(() -> service.deleteUnitIfAuthorized(id, UUID.randomUUID(), true));
    verify(unitRepo).deleteById(id);
  }

  @Test
  void deleteUnitIfAuthorized_nonAdminCannotDeleteGlobalUnit() {
    UUID id = UUID.randomUUID();
    Unit global = new Unit(id, UUID.randomUUID(), "Algebra", "desc", 1, Visibility.GLOBAL, null);
    when(unitRepo.findById(id)).thenReturn(Optional.of(global));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteUnitIfAuthorized(id, UUID.randomUUID(), false));
    verify(unitRepo, never()).deleteById(any());
  }

  @Test
  void deleteUnitIfAuthorized_ownerDeletesPrivateUnit() {
    UUID ownerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Unit priv = new Unit(id, UUID.randomUUID(), "My Algebra", "desc", 1, Visibility.PRIVATE, ownerId);
    when(unitRepo.findById(id)).thenReturn(Optional.of(priv));

    assertDoesNotThrow(() -> service.deleteUnitIfAuthorized(id, ownerId, false));
    verify(unitRepo).deleteById(id);
  }

  @Test
  void deleteUnitIfAuthorized_nonOwnerCannotDeletePrivateUnit() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Unit priv = new Unit(id, UUID.randomUUID(), "My Algebra", "desc", 1, Visibility.PRIVATE, ownerId);
    when(unitRepo.findById(id)).thenReturn(Optional.of(priv));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteUnitIfAuthorized(id, callerId, false));
    verify(unitRepo, never()).deleteById(any());
  }

  @Test
  void deleteUnitIfAuthorized_throwsWhenUnitNotFound() {
    UUID id = UUID.randomUUID();
    when(unitRepo.findById(id)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteUnitIfAuthorized(id, UUID.randomUUID(), true));
  }

  // ===================== Question =====================

  @Test
  void deleteQuestionIfAuthorized_adminDeletesGlobalQuestion() {
    UUID id = UUID.randomUUID();
    Question global = new Question(id, UUID.randomUUID(), "Q?", "exp", Question.Difficulty.EASY, Visibility.GLOBAL, null);
    when(questionRepo.findById(id)).thenReturn(Optional.of(global));

    assertDoesNotThrow(() -> service.deleteQuestionIfAuthorized(id, UUID.randomUUID(), true));
    verify(questionRepo).deleteById(id);
  }

  @Test
  void deleteQuestionIfAuthorized_nonAdminCannotDeleteGlobalQuestion() {
    UUID id = UUID.randomUUID();
    Question global = new Question(id, UUID.randomUUID(), "Q?", "exp", Question.Difficulty.EASY, Visibility.GLOBAL, null);
    when(questionRepo.findById(id)).thenReturn(Optional.of(global));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteQuestionIfAuthorized(id, UUID.randomUUID(), false));
    verify(questionRepo, never()).deleteById(any());
  }

  @Test
  void deleteQuestionIfAuthorized_ownerDeletesPrivateQuestion() {
    UUID ownerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Question priv = new Question(id, UUID.randomUUID(), "My Q?", "exp", Question.Difficulty.EASY, Visibility.PRIVATE, ownerId);
    when(questionRepo.findById(id)).thenReturn(Optional.of(priv));

    assertDoesNotThrow(() -> service.deleteQuestionIfAuthorized(id, ownerId, false));
    verify(questionRepo).deleteById(id);
  }

  @Test
  void deleteQuestionIfAuthorized_nonOwnerCannotDeletePrivateQuestion() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Question priv = new Question(id, UUID.randomUUID(), "My Q?", "exp", Question.Difficulty.EASY, Visibility.PRIVATE, ownerId);
    when(questionRepo.findById(id)).thenReturn(Optional.of(priv));

    assertThrows(AccessDeniedException.class,
        () -> service.deleteQuestionIfAuthorized(id, callerId, false));
    verify(questionRepo, never()).deleteById(any());
  }

  @Test
  void deleteQuestionIfAuthorized_throwsWhenQuestionNotFound() {
    UUID id = UUID.randomUUID();
    when(questionRepo.findById(id)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
        () -> service.deleteQuestionIfAuthorized(id, UUID.randomUUID(), true));
  }

  // ===================== Scope query delegates =====================

  @Test
  void getSubjectsByScopeDelegatesToRepo() {
    UUID userId = UUID.randomUUID();
    Subject s = Subject.createGlobal("Math", "desc");
    when(subjectRepo.findByScope(userId, Visibility.GLOBAL)).thenReturn(List.of(s));

    List<Subject> result = service.getSubjectsByScope(userId, true, Visibility.GLOBAL);

    assertEquals(1, result.size());
    verify(subjectRepo).findByScope(userId, Visibility.GLOBAL);
  }

  @Test
  void getVisibleUnitsBySubjectDelegatesToRepo() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit u = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(unitRepo.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of(u));

    List<Unit> result = service.getVisibleUnitsBySubject(subjectId, userId);

    assertEquals(1, result.size());
    verify(unitRepo).findVisibleBySubjectIdAndUserId(subjectId, userId);
  }

  @Test
  void getUnitsByScopeDelegatesToRepo() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit u = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    when(unitRepo.findBySubjectIdAndScope(subjectId, userId, Visibility.GLOBAL)).thenReturn(List.of(u));

    List<Unit> result = service.getUnitsByScope(subjectId, userId, true, Visibility.GLOBAL);

    assertEquals(1, result.size());
    verify(unitRepo).findBySubjectIdAndScope(subjectId, userId, Visibility.GLOBAL);
  }

  @Test
  void getVisibleQuestionsByUnitDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.MEDIUM);
    when(questionRepo.findVisibleByUnitIdAndUserId(unitId, userId)).thenReturn(List.of(q));

    List<Question> result = service.getVisibleQuestionsByUnit(unitId, userId);

    assertEquals(1, result.size());
    verify(questionRepo).findVisibleByUnitIdAndUserId(unitId, userId);
  }

  @Test
  void getQuestionsByScopeDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Question q = Question.createGlobal(unitId, "Q?", "exp", Question.Difficulty.HARD);
    org.springframework.data.domain.Page<Question> page =
        new org.springframework.data.domain.PageImpl<>(List.of(q));
    when(questionRepo.findPageByUnitIdAndScope(unitId, userId, Visibility.GLOBAL, 0, 10)).thenReturn(page);

    org.springframework.data.domain.Page<Question> result =
        service.getQuestionsByScope(unitId, userId, true, Visibility.GLOBAL, 0, 10);

    assertEquals(1, result.getTotalElements());
    verify(questionRepo).findPageByUnitIdAndScope(unitId, userId, Visibility.GLOBAL, 0, 10);
  }
}
