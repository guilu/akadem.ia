package com.akdemya.domain.port.out;

import com.akdemya.domain.model.ExamAttemptAnswer;
import java.util.List;
import java.util.UUID;

public interface ExamAttemptAnswerRepository {
  ExamAttemptAnswer save(ExamAttemptAnswer answer);

  void saveAll(List<ExamAttemptAnswer> answers);

  List<ExamAttemptAnswer> findByAttemptId(UUID attemptId);
}
