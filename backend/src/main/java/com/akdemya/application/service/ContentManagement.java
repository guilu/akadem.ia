package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContentManagement {
  private final SubjectRepository subjectRepo;
  private final UnitRepository unitRepo;
  private final QuestionRepository questionRepo;
  private final AnswerRepository answerRepo;

  public ContentManagement(SubjectRepository subjectRepo, UnitRepository unitRepo, QuestionRepository questionRepo,
      AnswerRepository answerRepo) {
    this.subjectRepo = subjectRepo;
    this.unitRepo = unitRepo;
    this.questionRepo = questionRepo;
    this.answerRepo = answerRepo;
  }

  public List<Subject> getAllSubjects() {
    return subjectRepo.findAll();
  }

  public List<Subject> getVisibleSubjects(UUID userId) {
    return subjectRepo.findVisibleByUserId(userId);
  }

  public Subject createSubject(Subject s) {
    return subjectRepo.save(s);
  }

  public void deleteSubject(UUID id) {
    subjectRepo.deleteById(id);
  }

  public List<Unit> getUnitsBySubject(UUID subjectId) {
    return unitRepo.findBySubjectId(subjectId);
  }

  public List<Unit> getVisibleUnitsBySubject(UUID subjectId, UUID userId) {
    return unitRepo.findVisibleBySubjectIdAndUserId(subjectId, userId);
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

  public List<Question> getQuestionsByUnit(UUID unitId) {
    return questionRepo.findByUnitId(unitId);
  }

  public List<Question> getVisibleQuestionsByUnit(UUID unitId, UUID userId) {
    return questionRepo.findVisibleByUnitIdAndUserId(unitId, userId);
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
