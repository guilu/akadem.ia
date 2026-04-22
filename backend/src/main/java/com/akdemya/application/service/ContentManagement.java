package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.out.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContentManagement {
  private final SubjectRepository subjectRepo;
  private final UnitRepository unitRepo;
  private final QuestionRepository questionRepo;
  private final AnswerRepository answerRepo;
  private final SyllabusRepository syllabusRepo;

  public ContentManagement(SubjectRepository subjectRepo, UnitRepository unitRepo, QuestionRepository questionRepo,
      AnswerRepository answerRepo, SyllabusRepository syllabusRepo) {
    this.subjectRepo = subjectRepo;
    this.unitRepo = unitRepo;
    this.questionRepo = questionRepo;
    this.answerRepo = answerRepo;
    this.syllabusRepo = syllabusRepo;
  }

  // ── Syllabus operations ──

  public List<Syllabus> getAllSyllabuses() {
    return syllabusRepo.findAll();
  }

  public List<Syllabus> getVisibleSyllabuses(UUID userId) {
    if (userId == null) {
      return syllabusRepo.findAll().stream()
          .filter(s -> s.getVisibility() == Visibility.GLOBAL)
          .toList();
    }
    return syllabusRepo.findVisibleByUserId(userId);
  }

  public List<Syllabus> getPrivateSyllabuses(UUID userId) {
    return syllabusRepo.findPrivateByOwnerId(userId);
  }

  public Optional<Syllabus> getSyllabusById(UUID id) {
    return syllabusRepo.findById(id);
  }

  public Syllabus createSyllabus(Syllabus syllabus) {
    return syllabusRepo.save(syllabus);
  }

  public void deleteSyllabus(UUID syllabusId, UUID callerId, boolean isAdmin) {
    Syllabus syllabus = syllabusRepo.findById(syllabusId)
        .orElseThrow(() -> new IllegalArgumentException("Syllabus not found: " + syllabusId));
    if (syllabus.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can delete GLOBAL syllabuses");
    }
    if (syllabus.getVisibility() == Visibility.PRIVATE && !syllabus.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot delete another user's PRIVATE syllabus");
    }
    List<Subject> linked = subjectRepo.findBySyllabusId(syllabusId);
    if (!linked.isEmpty()) {
      throw new IllegalStateException("Cannot delete syllabus with linked subjects");
    }
    syllabusRepo.deleteById(syllabusId);
  }

  public List<Subject> getSubjectsBySyllabus(UUID syllabusId, UUID callerId) {
    if (callerId == null) {
      return subjectRepo.findBySyllabusId(syllabusId).stream()
          .filter(s -> s.getVisibility() == Visibility.GLOBAL)
          .toList();
    }
    return subjectRepo.findVisibleBySyllabusId(syllabusId, callerId);
  }

  // ── Subject operations ──

  public List<Subject> getAllSubjects() {
    return subjectRepo.findAll();
  }

  public Optional<Subject> getSubjectById(UUID id) {
    return subjectRepo.findById(id);
  }

  public List<Subject> getVisibleSubjects(UUID userId) {
    return subjectRepo.findVisibleByUserId(userId);
  }

  /**
   * Returns subjects filtered by scope.
   * scope=null means ALL (GLOBAL + user's PRIVATE).
   * If scope=GLOBAL and isAdmin, returns all global subjects.
   * scope=PRIVATE → user's own private subjects only.
   */
  public List<Subject> getSubjectsByScope(UUID userId, boolean isAdmin, Visibility scope) {
    return subjectRepo.findByScope(userId, scope);
  }

  public Subject createSubject(Subject s) {
    return subjectRepo.save(s);
  }

  public void deleteSubject(UUID id) {
    subjectRepo.deleteById(id);
  }

  /**
   * Deletes a subject if the caller is authorized.
   * - GLOBAL: only admin can delete
   * - PRIVATE: only the owner can delete
   */
  public void deleteSubjectIfAuthorized(UUID id, UUID callerId, boolean isAdmin) {
    Subject subject = subjectRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
    if (subject.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can delete GLOBAL subjects");
    }
    if (subject.getVisibility() == Visibility.PRIVATE && !subject.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot delete another user's PRIVATE subject");
    }
    subjectRepo.deleteById(id);
  }

  public List<Unit> getUnitsBySubject(UUID subjectId) {
    return unitRepo.findBySubjectId(subjectId);
  }

  public Optional<Unit> getUnitById(UUID id) {
    return unitRepo.findById(id);
  }

  public List<Unit> getVisibleUnitsBySubject(UUID subjectId, UUID userId) {
    return unitRepo.findVisibleBySubjectIdAndUserId(subjectId, userId);
  }

  /** Returns units for a subject filtered by scope. */
  public List<Unit> getUnitsByScope(UUID subjectId, UUID userId, boolean isAdmin, Visibility scope) {
    return unitRepo.findBySubjectIdAndScope(subjectId, userId, scope);
  }

  public List<UnitAvailability> getUnitAvailability(UUID subjectId) {
    return getUnitAvailability(subjectId, null);
  }

  public List<UnitAvailability> getUnitAvailability(UUID subjectId, Question.Difficulty difficulty) {
    return unitRepo.findBySubjectId(subjectId).stream()
        .map(u -> new UnitAvailability(u.getId(), u.getName(),
            difficulty == null ? questionRepo.countByUnitId(u.getId())
                : questionRepo.countByUnitIdAndDifficulty(u.getId(), difficulty.name())))
        .toList();
  }

  /**
   * Returns visible unit availability for the caller: only GLOBAL units plus the caller's own
   * PRIVATE units are included, and question counts are scoped to visible questions per unit.
   */
  public List<UnitAvailability> getVisibleUnitAvailability(UUID subjectId, UUID callerId,
                                                           Question.Difficulty difficulty) {
    return unitRepo.findVisibleBySubjectIdAndUserId(subjectId, callerId).stream()
        .map(u -> {
          long count = difficulty == null
              ? questionRepo.findVisibleByUnitIdAndUserId(u.getId(), callerId).size()
              : questionRepo.findVisibleByUnitIdAndUserId(u.getId(), callerId).stream()
                  .filter(q -> q.getDifficulty().name().equalsIgnoreCase(difficulty.name()))
                  .count();
          return new UnitAvailability(u.getId(), u.getName(), count);
        })
        .toList();
  }

  public record UnitAvailability(UUID id, String name, long available) {}

  /**
   * Creates a unit, enforcing parent-child invariants:
   * - A PRIVATE unit cannot belong to a GLOBAL subject.
   * - A GLOBAL unit cannot belong to a PRIVATE subject.
   * - A PRIVATE unit must be owned by the same owner as its parent subject.
   */
  public Unit createUnit(Unit u) {
    Subject parent = subjectRepo.findById(u.getSubjectId())
        .orElseThrow(() -> new IllegalArgumentException("Parent subject not found: " + u.getSubjectId()));
    validateParentChildVisibility(parent.getVisibility(), parent.getOwnerId(),
        u.getVisibility(), u.getOwnerId(), "unit", "subject");
    return unitRepo.save(u);
  }

  public void deleteUnit(UUID id) {
    unitRepo.deleteById(id);
  }

  /**
   * Deletes a unit if the caller is authorized.
   * - GLOBAL: only admin can delete
   * - PRIVATE: only the owner can delete
   */
  public void deleteUnitIfAuthorized(UUID id, UUID callerId, boolean isAdmin) {
    Unit unit = unitRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + id));
    if (unit.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can delete GLOBAL units");
    }
    if (unit.getVisibility() == Visibility.PRIVATE && !unit.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot delete another user's PRIVATE unit");
    }
    unitRepo.deleteById(id);
  }

  public List<Question> getQuestionsByUnit(UUID unitId) {
    return questionRepo.findByUnitId(unitId);
  }

  public List<Question> getAllQuestions() {
    return questionRepo.findAll();
  }

  public Optional<Question> getQuestionById(UUID id) {
    return questionRepo.findById(id);
  }

  public List<Question> getVisibleQuestionsByUnit(UUID unitId, UUID userId) {
    return questionRepo.findVisibleByUnitIdAndUserId(unitId, userId);
  }

  public List<Question> getVisibleQuestions(UUID userId) {
    return questionRepo.findVisibleByUserId(userId);
  }

  /** Returns paginated questions for a unit filtered by scope. */
  public org.springframework.data.domain.Page<Question> getQuestionsByScope(
      UUID unitId, UUID userId, boolean isAdmin, Visibility scope, int page, int size) {
    return questionRepo.findPageByUnitIdAndScope(unitId, userId, scope, page, size);
  }

  /**
   * Creates a question, enforcing parent-child invariants:
   * - A PRIVATE question cannot belong to a GLOBAL unit.
   * - A GLOBAL question cannot belong to a PRIVATE unit.
   * - A PRIVATE question must be owned by the same owner as its parent unit.
   */
  public Question createQuestion(Question q) {
    Unit parent = unitRepo.findById(q.getUnitId())
        .orElseThrow(() -> new IllegalArgumentException("Parent unit not found: " + q.getUnitId()));
    validateParentChildVisibility(parent.getVisibility(), parent.getOwnerId(),
        q.getVisibility(), q.getOwnerId(), "question", "unit");
    return questionRepo.save(q);
  }

  public void deleteQuestion(UUID id) {
    questionRepo.deleteById(id);
  }

  /**
   * Deletes a question if the caller is authorized.
   * - GLOBAL: only admin can delete
   * - PRIVATE: only the owner can delete
   */
  public void deleteQuestionIfAuthorized(UUID id, UUID callerId, boolean isAdmin) {
    Question question = questionRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    if (question.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can delete GLOBAL questions");
    }
    if (question.getVisibility() == Visibility.PRIVATE && !question.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot delete another user's PRIVATE question");
    }
    questionRepo.deleteById(id);
  }

  public List<Answer> getAnswersByQuestion(UUID questionId) {
    return answerRepo.findByQuestionId(questionId);
  }

  public Answer createAnswer(Answer a) {
    long count = answerRepo.findByQuestionId(a.getQuestionId()).size();
    if (count >= 4) {
      throw new IllegalArgumentException("Question cannot have more than 4 answers");
    }
    return answerRepo.save(a);
  }

  public void deleteAnswer(UUID id) {
    answerRepo.deleteById(id);
  }

  public void deleteAnswersByQuestion(UUID qId) {
    answerRepo.deleteByQuestionId(qId);
  }

  /**
   * Validates that a child entity's visibility is compatible with its parent's visibility.
   * Rules:
   * - GLOBAL child under PRIVATE parent is forbidden.
   * - PRIVATE child under GLOBAL parent is forbidden.
   * - PRIVATE child must share owner with its PRIVATE parent.
   */
  private void validateParentChildVisibility(
      Visibility parentVisibility, UUID parentOwnerId,
      Visibility childVisibility, UUID childOwnerId,
      String childType, String parentType) {

    if (parentVisibility == Visibility.PRIVATE && childVisibility == Visibility.GLOBAL) {
      throw new IllegalArgumentException(
          "Cannot create a GLOBAL " + childType + " under a PRIVATE " + parentType);
    }
    if (parentVisibility == Visibility.GLOBAL && childVisibility == Visibility.PRIVATE) {
      throw new IllegalArgumentException(
          "Cannot create a PRIVATE " + childType + " under a GLOBAL " + parentType);
    }
    if (parentVisibility == Visibility.PRIVATE && childVisibility == Visibility.PRIVATE) {
      if (parentOwnerId == null || !parentOwnerId.equals(childOwnerId)) {
        throw new IllegalArgumentException(
            "A PRIVATE " + childType + " must be owned by the same user as its PRIVATE " + parentType);
      }
    }
  }
}
