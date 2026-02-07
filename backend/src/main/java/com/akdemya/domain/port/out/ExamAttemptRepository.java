package com.akdemya.domain.port.out;

import com.akdemya.domain.model.ExamAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
  ExamAttempt save(ExamAttempt attempt);

  Optional<ExamAttempt> findById(UUID id);

  List<ExamAttempt> findByUserEmail(String userEmail);

  void deleteById(UUID id);
}
