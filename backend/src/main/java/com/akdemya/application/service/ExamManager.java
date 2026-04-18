package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.in.ExamUseCase;
import com.akdemya.domain.port.out.*;
import com.akdemya.domain.model.StudySettingsDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamManager implements ExamUseCase {

  private final ExamAttemptRepository attemptRepo;
  private final ExamAttemptAnswerRepository attemptAnsRepo;
  private final QuestionRepository questionRepo;
  private final AnswerRepository answerRepo;
  private final UnitRepository unitRepo;
  private final SubjectRepository subjectRepo;
  private final UserRepository userRepo;
  private final UserSettingsRepository userSettingsRepo;
  private final Random rnd = new Random();
  private final ExamScoringCalculator scoringCalculator = new ExamScoringCalculator();

  public ExamManager(ExamAttemptRepository attemptRepo, ExamAttemptAnswerRepository attemptAnsRepo,
      QuestionRepository questionRepo, AnswerRepository answerRepo, UnitRepository unitRepo,
      SubjectRepository subjectRepo, UserRepository userRepo, UserSettingsRepository userSettingsRepo) {
    this.attemptRepo = attemptRepo;
    this.attemptAnsRepo = attemptAnsRepo;
    this.questionRepo = questionRepo;
    this.answerRepo = answerRepo;
    this.unitRepo = unitRepo;
    this.subjectRepo = subjectRepo;
    this.userRepo = userRepo;
    this.userSettingsRepo = userSettingsRepo;
  }

  private int getPenaltyRatioForEmail(String userEmail) {
    return userRepo.findByEmail(userEmail)
        .flatMap(u -> userSettingsRepo.findByUserId(u.getId()))
        .map(UserSettings::penaltyRatio)
        .orElse(StudySettingsDefaults.DEFAULT_PENALTY_RATIO);
  }

  @Override
  public StartResponse startExam(StartCommand command) {
    // Logic from Controller: map minutes -> totalSec, create attempt
    int totalSec = Math.max(60, command.minutes() * 60);
    ExamAttempt attempt = ExamAttempt.start(command.userEmail());
    // We need to set totalSeconds explicitly or use a builder/method.
    // Domain model 'start' factory sets finishedAt=null, score=null.
    // ExamAttemptEntity constructor took userEmail and totalTimeSeconds.
    // ExamAttempt domain constructor takes all.
    // I'll assume I can set totalTimeSeconds via constructor or I should've added a
    // setter or factory method arg.
    // Let's create a new instance with correct values instead of start() factory if
    // it's limited.
    // OR add logic to factory.
    // I'll just use constructor for now to be safe as I defined it.
    attempt = new ExamAttempt(UUID.randomUUID(), command.userEmail(), java.time.OffsetDateTime.now(), null, totalSec,
        null);

    attemptRepo.save(attempt);

    Question.Difficulty difficulty = null;
    if (command.difficulty() != null && !command.difficulty().isBlank()) {
      try {
        difficulty = Question.Difficulty.valueOf(command.difficulty());
      } catch (IllegalArgumentException ignored) {
      }
    }
    final Question.Difficulty selectedDifficulty = difficulty;

    List<Question> pool = new ArrayList<>();
    for (Map.Entry<UUID, Integer> entry : command.unitCounts().entrySet()) {
      UUID unitId = entry.getKey();
      int count = entry.getValue() != null ? entry.getValue() : 0;
      List<Question> qs = questionRepo.findVisibleByUnitIdAndUserId(unitId, command.userId());
      if (selectedDifficulty != null) {
        qs = qs.stream().filter(q -> q.getDifficulty() == selectedDifficulty).collect(Collectors.toList());
      }
      Collections.shuffle(qs, rnd);
      if (count > 0 && count < qs.size()) {
        qs = qs.subList(0, count);
      }
      pool.addAll(qs);
    }
    Collections.shuffle(pool, rnd);

    List<ExamAttemptAnswer> answerPlaceholders = new ArrayList<>();
    for (Question q : pool) {
      ExamAttemptAnswer ans = ExamAttemptAnswer.create(attempt.getId(), q.getId(), null);
      answerPlaceholders.add(ans);
    }
    // Save answers. Repo has saveAll?
    // My ExamAttemptAnswerRepository Port has saveAll.
    attemptAnsRepo.saveAll(answerPlaceholders);

    List<QuestionData> questionDataList = pool.stream().map(q -> {
      List<AnswerData> answers = answerRepo.findByQuestionId(q.getId()).stream()
          .map(a -> new AnswerData(a.getId(), a.getText()))
          .collect(Collectors.toList());
      Collections.shuffle(answers, rnd);
      return new QuestionData(q.getId(), q.getText(), answers);
    }).toList();

    return new StartResponse(attempt.getId(), totalSec, questionDataList);
  }

  @Override
  public StartResponse startRandomExam(StartRandomCommand command) {
    int totalSec = Math.max(60, command.minutes() * 60);
    ExamAttempt attempt = new ExamAttempt(UUID.randomUUID(), command.userEmail(),
        java.time.OffsetDateTime.now(), null, totalSec, null);
    attemptRepo.save(attempt);

    Question.Difficulty difficulty = null;
    if (command.difficulty() != null && !command.difficulty().isBlank()) {
      try {
        difficulty = Question.Difficulty.valueOf(command.difficulty());
      } catch (IllegalArgumentException ignored) {
      }
    }
    final Question.Difficulty selectedDifficulty = difficulty;

    List<Unit> units = unitRepo.findVisibleBySubjectIdAndUserId(command.subjectId(), command.userId());
    List<Question> allQuestions = new ArrayList<>();
    for (Unit u : units) {
      List<Question> qs = questionRepo.findVisibleByUnitIdAndUserId(u.getId(), command.userId());
      if (selectedDifficulty != null) {
        qs = qs.stream().filter(q -> q.getDifficulty() == selectedDifficulty).collect(Collectors.toList());
      }
      allQuestions.addAll(qs);
    }
    Collections.shuffle(allQuestions, rnd);
    int take = command.count() > 0 ? Math.min(command.count(), allQuestions.size()) : allQuestions.size();
    List<Question> pool = allQuestions.subList(0, take);

    List<ExamAttemptAnswer> placeholders = new ArrayList<>();
    for (Question q : pool) {
      placeholders.add(ExamAttemptAnswer.create(attempt.getId(), q.getId(), null));
    }
    attemptAnsRepo.saveAll(placeholders);

    List<QuestionData> questionDataList = pool.stream().map(q -> {
      List<AnswerData> answers = answerRepo.findByQuestionId(q.getId()).stream()
          .map(a -> new AnswerData(a.getId(), a.getText()))
          .collect(Collectors.toList());
      Collections.shuffle(answers, rnd);
      return new QuestionData(q.getId(), q.getText(), answers);
    }).toList();

    return new StartResponse(attempt.getId(), totalSec, questionDataList);
  }

  @Override
  public StartResponse startExamFromSyllabus(StartFromSyllabusCommand command) {
    int totalSec = Math.max(60, command.minutes() * 60);
    ExamAttempt attempt = new ExamAttempt(UUID.randomUUID(), command.userEmail(),
        java.time.OffsetDateTime.now(), null, totalSec, null);
    attemptRepo.save(attempt);

    Question.Difficulty difficulty = null;
    if (command.difficulty() != null && !command.difficulty().isBlank()) {
      try {
        difficulty = Question.Difficulty.valueOf(command.difficulty());
      } catch (IllegalArgumentException ignored) {
      }
    }
    final Question.Difficulty selectedDifficulty = difficulty;

    // Load subjects for the syllabus, filtering by subjectIds if provided
    List<com.akdemya.domain.model.Subject> subjects =
        subjectRepo.findVisibleBySyllabusId(command.syllabusId(), command.userId());
    if (command.subjectIds() != null && !command.subjectIds().isEmpty()) {
      java.util.Set<UUID> filter = new java.util.HashSet<>(command.subjectIds());
      subjects = subjects.stream().filter(s -> filter.contains(s.getId())).collect(Collectors.toList());
    }

    if (subjects.isEmpty()) {
      throw new java.util.NoSuchElementException("No subjects found for the given syllabus");
    }

    // Gather all visible questions per subject
    Map<UUID, List<Question>> questionsBySubject = new LinkedHashMap<>();
    for (com.akdemya.domain.model.Subject subject : subjects) {
      List<Unit> units = unitRepo.findVisibleBySubjectIdAndUserId(subject.getId(), command.userId());
      List<Question> subjectQuestions = new ArrayList<>();
      for (Unit unit : units) {
        List<Question> qs = questionRepo.findVisibleByUnitIdAndUserId(unit.getId(), command.userId());
        if (selectedDifficulty != null) {
          qs = qs.stream().filter(q -> q.getDifficulty() == selectedDifficulty).collect(Collectors.toList());
        }
        subjectQuestions.addAll(qs);
      }
      if (!subjectQuestions.isEmpty()) {
        questionsBySubject.put(subject.getId(), subjectQuestions);
      }
    }

    int totalAvailable = questionsBySubject.values().stream().mapToInt(List::size).sum();
    if (totalAvailable == 0) {
      throw new java.util.NoSuchElementException("No questions available for the given syllabus and filters");
    }

    // Distribute count proportionally across subjects, then pick randomly within each
    int totalCount = command.count() > 0 ? Math.min(command.count(), totalAvailable) : totalAvailable;
    List<Question> pool = new ArrayList<>();

    int remaining = totalCount;
    List<Map.Entry<UUID, List<Question>>> entries = new ArrayList<>(questionsBySubject.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      List<Question> subjectQs = new ArrayList<>(entries.get(i).getValue());
      int quota = (i == entries.size() - 1)
          ? remaining
          : (int) Math.round((double) subjectQs.size() / totalAvailable * totalCount);
      quota = Math.min(quota, subjectQs.size());
      quota = Math.min(quota, remaining);
      Collections.shuffle(subjectQs, rnd);
      pool.addAll(subjectQs.subList(0, quota));
      remaining -= quota;
      if (remaining <= 0) break;
    }
    Collections.shuffle(pool, rnd);

    List<ExamAttemptAnswer> placeholders = new ArrayList<>();
    for (Question q : pool) {
      placeholders.add(ExamAttemptAnswer.create(attempt.getId(), q.getId(), null));
    }
    attemptAnsRepo.saveAll(placeholders);

    List<QuestionData> questionDataList = pool.stream().map(q -> {
      List<AnswerData> answers = answerRepo.findByQuestionId(q.getId()).stream()
          .map(a -> new AnswerData(a.getId(), a.getText()))
          .collect(Collectors.toList());
      Collections.shuffle(answers, rnd);
      return new QuestionData(q.getId(), q.getText(), answers);
    }).toList();

    return new StartResponse(attempt.getId(), totalSec, questionDataList);
  }

  @Override
  public SubmitResult submitExam(SubmitCommand command, String userEmail) {
    ExamAttempt attempt = attemptRepo.findById(command.attemptId())
        .orElseThrow(() -> new NoSuchElementException("Attempt not found"));
    if (!attempt.getUserEmail().equals(userEmail)) {
      throw new SecurityException("Forbidden");
    }

    List<ExamAttemptAnswer> entries = attemptAnsRepo.findByAttemptId(command.attemptId());

    if (attempt.getFinishedAt() == null) {
      // Update selections
      List<ExamAttemptAnswer> updatedEntries = new ArrayList<>();
      for (ExamAttemptAnswer e : entries) {
        UUID sel = command.selections().get(e.getQuestionId());
        if (sel != null) {
          // Domain objects are immutable → create new instance with updated answerId.
          ExamAttemptAnswer updated = new ExamAttemptAnswer(e.getId(), e.getExamAttemptId(), e.getQuestionId(), sel);
          updatedEntries.add(updated);
        } else {
          updatedEntries.add(e);
        }
      }
      attemptAnsRepo.saveAll(updatedEntries);
      entries = updatedEntries;
    }

    // Compute score
    int total = entries.size();
    int correct = 0;
    int wrong = 0;
    for (ExamAttemptAnswer e : entries) {
      if (e.getAnswerId() == null)
        continue;
      Answer ans = answerRepo.findById(e.getAnswerId()).orElse(null);
      if (ans != null && ans.isCorrect()) {
        correct++;
      } else if (ans != null) {
        wrong++;
      }
    }

    int penaltyRatio = getPenaltyRatioForEmail(userEmail);
    ExamScoringCalculator.ScoringResult scoring = scoringCalculator.compute(total, correct, wrong, penaltyRatio);

    if (attempt.getFinishedAt() == null) {
      attempt.finish(attempt.getTotalTimeSeconds(), scoring.net());
      attemptRepo.save(attempt);
    }

    return new SubmitResult(scoring.total(), scoring.correct(), scoring.wrong(), scoring.penalty(), scoring.net(),
        scoring.percentage());
  }

  @Override
  public AttemptResponse getAttempt(UUID attemptId, String userEmail) {
    ExamAttempt attempt = attemptRepo.findById(attemptId)
        .orElseThrow(() -> new NoSuchElementException("Attempt not found"));
    if (!attempt.getUserEmail().equals(userEmail)) {
      throw new SecurityException("Forbidden");
    }

    List<ExamAttemptAnswer> entries = attemptAnsRepo.findByAttemptId(attemptId);

    List<AttemptQuestionData> questions = entries.stream().map(e -> {
      Question q = questionRepo.findById(e.getQuestionId())
          .orElseThrow(() -> new IllegalArgumentException("Invalid question ID"));
      List<AnswerData> answers = answerRepo.findByQuestionId(q.getId()).stream()
          .map(a -> new AnswerData(a.getId(), a.getText()))
          .collect(Collectors.toList());
      return new AttemptQuestionData(q.getId(), q.getText(), answers, e.getAnswerId());
    }).toList();

    int nextIndex = 0;
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).getAnswerId() == null) {
        nextIndex = i;
        break;
      }
      nextIndex = entries.size();
    }

    boolean finished = attempt.getFinishedAt() != null;
    return new AttemptResponse(attempt.getId(), attempt.getTotalTimeSeconds() == null ? 0 : attempt.getTotalTimeSeconds(),
        questions, nextIndex, finished);
  }

  @Override
  public List<AttemptSummary> listAttempts(String userEmail) {
    List<ExamAttempt> attempts = new ArrayList<>(attemptRepo.findByUserEmail(userEmail));
    attempts.sort(Comparator.comparing(ExamAttempt::getStartedAt).reversed());

    int penaltyRatio = getPenaltyRatioForEmail(userEmail);
    Map<UUID, Answer> answerCache = new HashMap<>();
    Map<UUID, Question> questionCache = new HashMap<>();
    Map<UUID, Unit> unitCache = new HashMap<>();
    Map<UUID, Subject> subjectCache = new HashMap<>();

    List<AttemptSummary> summaries = new ArrayList<>();
    for (ExamAttempt attempt : attempts) {
      List<ExamAttemptAnswer> entries = attemptAnsRepo.findByAttemptId(attempt.getId());
      int total = entries.size();
      int correct = 0;
      int wrong = 0;
      for (ExamAttemptAnswer e : entries) {
        if (e.getAnswerId() == null)
          continue;
        Answer ans = answerCache.computeIfAbsent(e.getAnswerId(), id -> answerRepo.findById(id).orElse(null));
        if (ans != null && ans.isCorrect()) {
          correct++;
        } else if (ans != null) {
          wrong++;
        }
      }
      ExamScoringCalculator.ScoringResult scoring = scoringCalculator.compute(total, correct, wrong, penaltyRatio);

      String subjectName = "Desconocida";
      if (!entries.isEmpty()) {
        Question q = questionCache.computeIfAbsent(entries.get(0).getQuestionId(),
            id -> questionRepo.findById(id).orElse(null));
        if (q != null) {
          Unit unit = unitCache.computeIfAbsent(q.getUnitId(), id -> unitRepo.findById(id).orElse(null));
          if (unit != null) {
            Subject subject = subjectCache.computeIfAbsent(unit.getSubjectId(), id -> subjectRepo.findById(id).orElse(null));
            if (subject != null) {
              subjectName = subject.getName();
            }
          }
        }
      }

      summaries.add(new AttemptSummary(
          attempt.getId(),
          subjectName,
          attempt.getStartedAt(),
          attempt.getFinishedAt(),
          attempt.getTotalTimeSeconds() == null ? 0 : attempt.getTotalTimeSeconds(),
          attempt.getScore(),
          total,
          correct,
          wrong,
          scoring.percentage()
      ));
    }

    return summaries;
  }

  @Override
  public void updateAnswer(UpdateAnswerCommand command, String userEmail) {
    ExamAttempt attempt = attemptRepo.findById(command.attemptId())
        .orElseThrow(() -> new NoSuchElementException("Attempt not found"));
    if (!attempt.getUserEmail().equals(userEmail)) {
      throw new SecurityException("Forbidden");
    }

    if (attempt.getFinishedAt() != null) {
      return;
    }

    ExamAttemptAnswer existing = attemptAnsRepo.findByAttemptIdAndQuestionId(command.attemptId(), command.questionId())
        .orElseThrow(() -> new IllegalArgumentException("Invalid question ID"));

    ExamAttemptAnswer updated = new ExamAttemptAnswer(existing.getId(), existing.getExamAttemptId(),
        existing.getQuestionId(), command.selectedAnswerId());
    attemptAnsRepo.save(updated);
  }

  @Override
  public SubmitResult getResult(UUID attemptId, String userEmail) {
    ExamAttempt attempt = attemptRepo.findById(attemptId)
        .orElseThrow(() -> new NoSuchElementException("Attempt not found"));
    if (!attempt.getUserEmail().equals(userEmail)) {
      throw new SecurityException("Forbidden");
    }
    if (attempt.getFinishedAt() == null) {
      throw new IllegalStateException("Exam not yet submitted");
    }
    List<ExamAttemptAnswer> entries = attemptAnsRepo.findByAttemptId(attemptId);
    int total = entries.size();
    int correct = 0;
    int wrong = 0;
    for (ExamAttemptAnswer e : entries) {
      if (e.getAnswerId() == null) continue;
      Answer ans = answerRepo.findById(e.getAnswerId()).orElse(null);
      if (ans != null && ans.isCorrect()) correct++;
      else if (ans != null) wrong++;
    }
    int penaltyRatio = getPenaltyRatioForEmail(userEmail);
    ExamScoringCalculator.ScoringResult scoring = scoringCalculator.compute(total, correct, wrong, penaltyRatio);
    return new SubmitResult(scoring.total(), scoring.correct(), scoring.wrong(), scoring.penalty(), scoring.net(),
        scoring.percentage());
  }
}
