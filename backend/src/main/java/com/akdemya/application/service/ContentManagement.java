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

  public Subject createSubject(Subject s) {
    return subjectRepo.save(s);
  }

  public void deleteSubject(UUID id) {
    subjectRepo.deleteById(id);
  }

  public List<Unit> getUnitsBySubject(UUID subjectId) {
    return unitRepo.findBySubjectId(subjectId);
  }

  public List<UnitAvailability> getUnitAvailability(UUID subjectId) {
    return getUnitAvailability(subjectId, null);
  }

  public List<UnitAvailability> getUnitAvailability(UUID subjectId, Question.Difficulty difficulty) {
    return unitRepo.findBySubjectId(subjectId).stream()
        .map(u -> new UnitAvailability(u.getId(), u.getName(),
            difficulty == null ? questionRepo.countByUnitId(u.getId())
                : questionRepo.countByUnitIdAndDifficulty(u.getId(), difficulty)))
        .toList();
  }

  public record UnitAvailability(UUID id, String name, long available) {}

  public Unit createUnit(Unit u) {
    return unitRepo.save(u);
  }

  public void deleteUnit(UUID id) {
    unitRepo.deleteById(id);
  }

  public List<Question> getQuestionsByUnit(UUID unitId) {
    return questionRepo.findByUnitId(unitId);
  }

  public Question createQuestion(Question q) {
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
}
