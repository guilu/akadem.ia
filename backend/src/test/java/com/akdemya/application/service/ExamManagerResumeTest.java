package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.out.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExamManagerResumeTest {

  @Test
  void updateAnswerAndGetAttemptNextIndex() {
    UUID attemptId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID q1Id = UUID.randomUUID();
    UUID q2Id = UUID.randomUUID();

    Question q1 = new Question(q1Id, unitId, "Q1", null, Question.Difficulty.EASY);
    Question q2 = new Question(q2Id, unitId, "Q2", null, Question.Difficulty.EASY);

    Answer a1 = new Answer(UUID.randomUUID(), q1Id, "A1", true);
    Answer a2 = new Answer(UUID.randomUUID(), q1Id, "A2", false);
    Answer b1 = new Answer(UUID.randomUUID(), q2Id, "B1", true);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(q1, q2));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(a1, a2, b1));

    ExamAttempt attempt = new ExamAttempt(attemptId, "test@akdemya.com", OffsetDateTime.now(), null, 120, null);
    attemptRepo.save(attempt);

    attemptAnswerRepo.saveAll(List.of(
        new ExamAttemptAnswer(UUID.randomUUID(), attemptId, q1Id, null),
        new ExamAttemptAnswer(UUID.randomUUID(), attemptId, q2Id, null)
    ));

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo);

    var initial = manager.getAttempt(attemptId);
    assertEquals(0, initial.nextQuestionIndex());
    assertNull(initial.questions().get(0).selectedAnswerId());

    manager.updateAnswer(new com.akdemya.domain.port.in.ExamUseCase.UpdateAnswerCommand(attemptId, q1Id, a1.getId()));

    var resumed = manager.getAttempt(attemptId);
    int expectedNext = 0;
    for (int i = 0; i < resumed.questions().size(); i++) {
      if (resumed.questions().get(i).selectedAnswerId() == null) {
        expectedNext = i;
        break;
      }
      expectedNext = resumed.questions().size();
    }
    assertEquals(expectedNext, resumed.nextQuestionIndex());
    assertTrue(resumed.questions().stream().anyMatch(q -> a1.getId().equals(q.selectedAnswerId())));
  }

  static class InMemoryAttemptRepo implements ExamAttemptRepository {
    private final Map<UUID, ExamAttempt> data = new ConcurrentHashMap<>();

    @Override
    public ExamAttempt save(ExamAttempt attempt) {
      data.put(attempt.getId(), attempt);
      return attempt;
    }

    @Override
    public Optional<ExamAttempt> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<ExamAttempt> findByUserEmail(String email) {
      return data.values().stream().filter(a -> a.getUserEmail().equals(email)).toList();
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }
  }

  static class InMemoryAttemptAnswerRepo implements ExamAttemptAnswerRepository {
    private final Map<UUID, ExamAttemptAnswer> data = new ConcurrentHashMap<>();

    @Override
    public ExamAttemptAnswer save(ExamAttemptAnswer answer) {
      data.put(answer.getId(), answer);
      return answer;
    }

    @Override
    public void saveAll(List<ExamAttemptAnswer> answers) {
      for (ExamAttemptAnswer a : answers) {
        data.put(a.getId(), a);
      }
    }

    @Override
    public List<ExamAttemptAnswer> findByAttemptId(UUID attemptId) {
      return data.values().stream()
          .filter(a -> a.getExamAttemptId().equals(attemptId))
          .sorted(Comparator.comparing(ExamAttemptAnswer::getId))
          .collect(Collectors.toList());
    }

    @Override
    public Optional<ExamAttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId) {
      return data.values().stream()
          .filter(a -> a.getExamAttemptId().equals(attemptId) && a.getQuestionId().equals(questionId))
          .findFirst();
    }
  }

  static class InMemoryQuestionRepo implements QuestionRepository {
    private final Map<UUID, Question> data = new ConcurrentHashMap<>();

    InMemoryQuestionRepo(List<Question> questions) {
      for (Question q : questions) data.put(q.getId(), q);
    }

    @Override
    public List<Question> findByUnitId(UUID unitId) {
      return data.values().stream().filter(q -> q.getUnitId().equals(unitId)).toList();
    }

    @Override
    public Optional<Question> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public Question save(Question question) {
      data.put(question.getId(), question);
      return question;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public long countByUnitId(UUID unitId) {
      return data.values().stream().filter(q -> q.getUnitId().equals(unitId)).count();
    }
  }

  static class InMemoryAnswerRepo implements AnswerRepository {
    private final Map<UUID, Answer> data = new ConcurrentHashMap<>();

    InMemoryAnswerRepo(List<Answer> answers) {
      for (Answer a : answers) data.put(a.getId(), a);
    }

    @Override
    public List<Answer> findByQuestionId(UUID questionId) {
      return data.values().stream().filter(a -> a.getQuestionId().equals(questionId)).toList();
    }

    @Override
    public Optional<Answer> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public Answer save(Answer answer) {
      data.put(answer.getId(), answer);
      return answer;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public void deleteByQuestionId(UUID questionId) {
      data.values().removeIf(a -> a.getQuestionId().equals(questionId));
    }
  }
}
