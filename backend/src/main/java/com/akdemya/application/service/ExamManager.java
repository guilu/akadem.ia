package com.akdemya.application.service;

import com.akdemya.domain.model.*;
import com.akdemya.domain.port.in.ExamUseCase;
import com.akdemya.domain.port.out.*;
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
  private final Random rnd = new Random();
  private final ExamScoringCalculator scoringCalculator = new ExamScoringCalculator();

  public ExamManager(ExamAttemptRepository attemptRepo, ExamAttemptAnswerRepository attemptAnsRepo,
      QuestionRepository questionRepo, AnswerRepository answerRepo) {
    this.attemptRepo = attemptRepo;
    this.attemptAnsRepo = attemptAnsRepo;
    this.questionRepo = questionRepo;
    this.answerRepo = answerRepo;
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

    List<Question> pool = new ArrayList<>();
    for (Map.Entry<UUID, Integer> entry : command.unitCounts().entrySet()) {
      UUID unitId = entry.getKey();
      int count = entry.getValue() != null ? entry.getValue() : 0;
      List<Question> qs = questionRepo.findByUnitId(unitId);
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
  public SubmitResult submitExam(SubmitCommand command) {
    ExamAttempt attempt = attemptRepo.findById(command.attemptId())
        .orElseThrow(() -> new IllegalArgumentException("Invalid attempt ID"));

    List<ExamAttemptAnswer> entries = attemptAnsRepo.findByAttemptId(command.attemptId());

    // Update selections
    List<ExamAttemptAnswer> updatedEntries = new ArrayList<>();
    for (ExamAttemptAnswer e : entries) {
      UUID sel = command.selections().get(e.getQuestionId());
      if (sel != null) {
        // Determine if we need to create a new instance or if we can mutate.
        // Domain objects are ideally immutable.
        // ExamAttemptAnswer fields are final.
        // So create new instance with updated answerId.
        ExamAttemptAnswer updated = new ExamAttemptAnswer(e.getId(), e.getExamAttemptId(), e.getQuestionId(), sel);
        updatedEntries.add(updated);
      } else {
        updatedEntries.add(e);
      }
    }
    attemptAnsRepo.saveAll(updatedEntries);

    // Compute score
    int total = updatedEntries.size();
    int correct = 0;
    int wrong = 0;
    for (ExamAttemptAnswer e : updatedEntries) {
      if (e.getAnswerId() == null)
        continue;
      // Fetch answer to check correctness.
      // Optimized: could fetch all answers involved or use a map.
      // For now simple loop.
      Answer ans = answerRepo.findById(e.getAnswerId()).orElse(null);
      if (ans != null && ans.isCorrect()) {
        correct++;
      } else if (ans != null) {
        wrong++;
      }
    }

    ExamScoringCalculator.ScoringResult scoring = scoringCalculator.compute(total, correct, wrong);

    attempt.finish(attempt.getTotalTimeSeconds(), scoring.net()); // Update attempt state
    attemptRepo.save(attempt);

    return new SubmitResult(scoring.total(), scoring.correct(), scoring.wrong(), scoring.penalty(), scoring.net(),
        scoring.percentage());
  }
}
