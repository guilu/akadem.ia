package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.in.UserSettingsUseCase;
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

    Subject subject = new Subject(UUID.randomUUID(), "Matematicas", null);
    Unit unit = new Unit(unitId, subject.getId(), "Unit", null, 1);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(q1, q2));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(a1, a2, b1));
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of(subject));

    ExamAttempt attempt = new ExamAttempt(attemptId, "test@akdemya.com", OffsetDateTime.now(), null, 120, null);
    attemptRepo.save(attempt);

    attemptAnswerRepo.saveAll(List.of(
        new ExamAttemptAnswer(UUID.randomUUID(), attemptId, q1Id, null),
        new ExamAttemptAnswer(UUID.randomUUID(), attemptId, q2Id, null)
    ));

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var initial = manager.getAttempt(attemptId, "test@akdemya.com");
    assertEquals(0, initial.nextQuestionIndex());
    assertNull(initial.questions().get(0).selectedAnswerId());

    manager.updateAnswer(new com.akdemya.domain.port.in.ExamUseCase.UpdateAnswerCommand(attemptId, q1Id, a1.getId()),
        "test@akdemya.com");

    var resumed = manager.getAttempt(attemptId, "test@akdemya.com");
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



  @Test
  void listAttemptsReturnsSummariesOrdered() {
    UUID subjectId = UUID.randomUUID();
    Subject subject = new Subject(subjectId, "Matematicas", null);
    UUID unitId = UUID.randomUUID();
    Unit unit = new Unit(unitId, subjectId, "Unit", null, 1);

    UUID q1Id = UUID.randomUUID();
    UUID q2Id = UUID.randomUUID();
    Question q1 = new Question(q1Id, unitId, "Q1", null, Question.Difficulty.EASY);
    Question q2 = new Question(q2Id, unitId, "Q2", null, Question.Difficulty.EASY);

    Answer a1 = new Answer(UUID.randomUUID(), q1Id, "A1", true);
    Answer a2 = new Answer(UUID.randomUUID(), q1Id, "A2", false);
    Answer b1 = new Answer(UUID.randomUUID(), q2Id, "B1", false);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(q1, q2));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(a1, a2, b1));
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of(subject));

    ExamAttempt attemptOld = new ExamAttempt(UUID.randomUUID(), "user@akdemya.com",
        OffsetDateTime.now().minusHours(2), OffsetDateTime.now().minusHours(1), 120, 0);
    ExamAttempt attemptNew = new ExamAttempt(UUID.randomUUID(), "user@akdemya.com",
        OffsetDateTime.now().minusHours(1), null, 120, null);

    attemptRepo.save(attemptOld);
    attemptRepo.save(attemptNew);

    attemptAnswerRepo.saveAll(List.of(
        new ExamAttemptAnswer(UUID.randomUUID(), attemptOld.getId(), q1Id, a1.getId()),
        new ExamAttemptAnswer(UUID.randomUUID(), attemptOld.getId(), q2Id, b1.getId()),
        new ExamAttemptAnswer(UUID.randomUUID(), attemptNew.getId(), q1Id, a2.getId())
    ));

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var summaries = manager.listAttempts("user@akdemya.com");
    assertEquals(2, summaries.size());
    assertEquals(attemptNew.getId(), summaries.get(0).attemptId());
    assertEquals("Matematicas", summaries.get(0).subjectName());
    assertEquals(1, summaries.get(0).totalQuestions());
    assertEquals(2, summaries.get(1).totalQuestions());
  }

  // ── Visibility tests ──────────────────────────────────────────────────────

  @Test
  void startExam_withGlobalUnit_returnsQuestions() {
    UUID callerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID qId = UUID.randomUUID();

    // GLOBAL unit and question — visible to everyone
    Unit unit = new Unit(unitId, UUID.randomUUID(), "Global Unit", null, 1,
        com.akdemya.domain.model.Visibility.GLOBAL, null);
    Question question = new Question(qId, unitId, "Q?", null, Question.Difficulty.EASY,
        com.akdemya.domain.model.Visibility.GLOBAL, null);
    Answer answer = new Answer(UUID.randomUUID(), qId, "A", true);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(question));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(answer));
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of());

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var command = new com.akdemya.domain.port.in.ExamUseCase.StartCommand(
        "user@test.com", callerId, Map.of(unitId, 10), 30, null);
    var response = manager.startExam(command);

    assertEquals(1, response.questions().size(), "Global questions must be included");
  }

  @Test
  void startExam_withOwnPrivateUnit_returnsQuestions() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID qId = UUID.randomUUID();

    // PRIVATE unit and question owned by the caller
    Unit unit = new Unit(unitId, UUID.randomUUID(), "Private Unit", null, 1,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);
    Question question = new Question(qId, unitId, "Q?", null, Question.Difficulty.EASY,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);
    Answer answer = new Answer(UUID.randomUUID(), qId, "A", true);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(question));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(answer));
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of());

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var command = new com.akdemya.domain.port.in.ExamUseCase.StartCommand(
        "owner@test.com", ownerId, Map.of(unitId, 10), 30, null);
    var response = manager.startExam(command);

    assertEquals(1, response.questions().size(), "Owner's own private questions must be included");
  }

  @Test
  void startExam_withAnotherUsersPrivateUnit_returnsNoQuestions() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID(); // different user
    UUID unitId = UUID.randomUUID();
    UUID qId = UUID.randomUUID();

    // PRIVATE unit and question owned by a different user
    Unit unit = new Unit(unitId, UUID.randomUUID(), "Private Unit", null, 1,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);
    Question question = new Question(qId, unitId, "Q?", null, Question.Difficulty.EASY,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(question));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of());
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of());

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var command = new com.akdemya.domain.port.in.ExamUseCase.StartCommand(
        "caller@test.com", callerId, Map.of(unitId, 10), 30, null);
    var response = manager.startExam(command);

    assertEquals(0, response.questions().size(),
        "Another user's private questions must not be accessible");
  }

  @Test
  void startRandomExam_withAnotherUsersPrivateUnit_returnsNoQuestions() {
    UUID ownerId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID(); // different user
    UUID subjectId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID qId = UUID.randomUUID();

    Subject subject = new Subject(subjectId, "Math", null);
    // PRIVATE unit owned by a different user
    Unit unit = new Unit(unitId, subjectId, "Private Unit", null, 1,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);
    Question question = new Question(qId, unitId, "Q?", null, Question.Difficulty.EASY,
        com.akdemya.domain.model.Visibility.PRIVATE, ownerId);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(question));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of());
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of(subject));

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var command = new com.akdemya.domain.port.in.ExamUseCase.StartRandomCommand(
        "caller@test.com", callerId, subjectId, 10, 30, null);
    var response = manager.startRandomExam(command);

    assertEquals(0, response.questions().size(),
        "Another user's private units/questions must not be accessible in random exam");
  }

  @Test
  void startRandomExam_withGlobalUnit_returnsQuestions() {
    UUID callerId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID qId = UUID.randomUUID();

    Subject subject = new Subject(subjectId, "Math", null);
    Unit unit = new Unit(unitId, subjectId, "Global Unit", null, 1,
        com.akdemya.domain.model.Visibility.GLOBAL, null);
    Question question = new Question(qId, unitId, "Q?", null, Question.Difficulty.EASY,
        com.akdemya.domain.model.Visibility.GLOBAL, null);
    Answer answer = new Answer(UUID.randomUUID(), qId, "A", true);

    InMemoryAttemptRepo attemptRepo = new InMemoryAttemptRepo();
    InMemoryAttemptAnswerRepo attemptAnswerRepo = new InMemoryAttemptAnswerRepo();
    InMemoryQuestionRepo questionRepo = new InMemoryQuestionRepo(List.of(question));
    InMemoryAnswerRepo answerRepo = new InMemoryAnswerRepo(List.of(answer));
    InMemoryUnitRepo unitRepo = new InMemoryUnitRepo(List.of(unit));
    InMemorySubjectRepo subjectRepo = new InMemorySubjectRepo(List.of(subject));

    ExamManager manager = new ExamManager(attemptRepo, attemptAnswerRepo, questionRepo, answerRepo, unitRepo, subjectRepo, new StubUserRepo(), new StubUserSettingsRepo());

    var command = new com.akdemya.domain.port.in.ExamUseCase.StartRandomCommand(
        "user@test.com", callerId, subjectId, 10, 30, null);
    var response = manager.startRandomExam(command);

    assertEquals(1, response.questions().size(), "Global questions must be included in random exam");
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
    public org.springframework.data.domain.Page<Question> findPageByUnitId(UUID unitId, int page, int size) {
      var list = data.values().stream().filter(q -> q.getUnitId().equals(unitId)).toList();
      int from = Math.min(page * size, list.size());
      int to = Math.min(from + size, list.size());
      return new org.springframework.data.domain.PageImpl<>(list.subList(from, to), org.springframework.data.domain.PageRequest.of(page, size), list.size());
    }

    @Override
    public java.util.List<Question> findAll() {
      return data.values().stream().toList();
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

    @Override
    public long countByUnitIdAndDifficulty(UUID unitId, String difficulty) {
      return data.values().stream()
          .filter(q -> q.getUnitId().equals(unitId) && q.getDifficulty() != null
              && q.getDifficulty().name().equals(difficulty))
          .count();
    }

    @Override
    public java.util.List<Question> findVisibleByUserId(UUID userId) {
      return data.values().stream()
          .filter(q -> q.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
              || userId.equals(q.getOwnerId()))
          .toList();
    }

    @Override
    public java.util.List<Question> findVisibleByUnitIdAndUserId(UUID unitId, UUID userId) {
      return data.values().stream()
          .filter(q -> q.getUnitId().equals(unitId)
              && (q.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
                  || userId.equals(q.getOwnerId())))
          .toList();
    }

    @Override
    public org.springframework.data.domain.Page<Question> findPageByUnitIdAndScope(UUID unitId, UUID userId, com.akdemya.domain.model.Visibility scope, int page, int size) {
      var list = data.values().stream()
          .filter(q -> q.getUnitId().equals(unitId))
          .filter(q -> {
            if (scope == null) return true;
            return switch (scope) {
              case GLOBAL -> q.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL;
              case PRIVATE -> q.getVisibility() == com.akdemya.domain.model.Visibility.PRIVATE && userId.equals(q.getOwnerId());
            };
          })
          .toList();
      int from = Math.min(page * size, list.size());
      int to = Math.min(from + size, list.size());
      return new org.springframework.data.domain.PageImpl<>(list.subList(from, to), org.springframework.data.domain.PageRequest.of(page, size), list.size());
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

  static class InMemoryUnitRepo implements UnitRepository {
    private final Map<UUID, Unit> data = new ConcurrentHashMap<>();

    InMemoryUnitRepo(List<Unit> units) {
      for (Unit u : units) data.put(u.getId(), u);
    }

    @Override
    public List<Unit> findBySubjectId(UUID subjectId) {
      return data.values().stream().filter(u -> u.getSubjectId().equals(subjectId)).toList();
    }

    @Override
    public List<Unit> findAll() {
      return new ArrayList<>(data.values());
    }

    @Override
    public List<Unit> findBySubjectIdWithFlashcards(UUID subjectId) {
      return findBySubjectId(subjectId);
    }

    @Override
    public List<Unit> findAllWithFlashcards() {
      return findAll();
    }

    @Override
    public Optional<Unit> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public Unit save(Unit unit) {
      data.put(unit.getId(), unit);
      return unit;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public java.util.List<Unit> findVisibleBySubjectIdAndUserId(UUID subjectId, UUID userId) {
      return data.values().stream()
          .filter(u -> u.getSubjectId().equals(subjectId)
              && (u.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
                  || userId.equals(u.getOwnerId())))
          .toList();
    }

    @Override
    public java.util.List<Unit> findBySubjectIdAndScope(UUID subjectId, UUID userId, com.akdemya.domain.model.Visibility scope) {
      return data.values().stream()
          .filter(u -> u.getSubjectId().equals(subjectId))
          .filter(u -> {
            if (scope == null) return true;
            return switch (scope) {
              case GLOBAL -> u.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL;
              case PRIVATE -> u.getVisibility() == com.akdemya.domain.model.Visibility.PRIVATE && userId.equals(u.getOwnerId());
            };
          })
          .toList();
    }
  }

  static class StubUserRepo implements UserRepository {
    @Override public Optional<com.akdemya.domain.model.AppUser> findByEmail(String email) { return Optional.empty(); }
    @Override public com.akdemya.domain.model.AppUser save(com.akdemya.domain.model.AppUser user) { return user; }
    @Override public Optional<com.akdemya.domain.model.AppUser> findById(UUID id) { return Optional.empty(); }
    @Override public boolean existsByEmail(String email) { return false; }
    @Override public List<com.akdemya.domain.model.AppUser> findAll() { return List.of(); }
    @Override public org.springframework.data.domain.Page<com.akdemya.domain.model.AppUser> findPage(int page, int size) { return org.springframework.data.domain.Page.empty(); }
    @Override public void deleteById(UUID id) {}
  }

  static class StubUserSettingsRepo implements UserSettingsRepository {
    @Override public Optional<com.akdemya.domain.model.UserSettings> findByUserId(UUID userId) { return Optional.empty(); }
    @Override public com.akdemya.domain.model.UserSettings save(com.akdemya.domain.model.UserSettings settings) { return settings; }
  }

  static class InMemorySubjectRepo implements SubjectRepository {
    private final Map<UUID, Subject> data = new ConcurrentHashMap<>();

    InMemorySubjectRepo(List<Subject> subjects) {
      for (Subject s : subjects) data.put(s.getId(), s);
    }

    @Override
    public List<Subject> findAll() {
      return new ArrayList<>(data.values());
    }

    @Override
    public Optional<Subject> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public Subject save(Subject subject) {
      data.put(subject.getId(), subject);
      return subject;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public List<Subject> findVisibleByUserId(UUID userId) {
      return data.values().stream()
          .filter(s -> s.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
              || userId.equals(s.getOwnerId()))
          .toList();
    }

    @Override
    public List<Subject> findByScope(UUID userId, com.akdemya.domain.model.Visibility scope) {
      return data.values().stream()
          .filter(s -> {
            if (scope == null) return true;
            return switch (scope) {
              case GLOBAL -> s.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL;
              case PRIVATE -> s.getVisibility() == com.akdemya.domain.model.Visibility.PRIVATE && userId.equals(s.getOwnerId());
            };
          })
          .toList();
    }
  }
}
