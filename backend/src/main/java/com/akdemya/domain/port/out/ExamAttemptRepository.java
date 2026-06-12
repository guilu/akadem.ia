package com.akdemya.domain.port.out;

import com.akdemya.domain.model.ExamAttempt;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository {
  ExamAttempt save(ExamAttempt attempt);

  Optional<ExamAttempt> findById(UUID id);

  List<ExamAttempt> findByUserEmail(String userEmail);

  void deleteById(UUID id);

  /**
   * Atomically marks the attempt as finished if it is not finished yet.
   *
   * @return number of rows updated: 1 when this call finished the attempt,
   *         0 when a concurrent submit already finished it
   */
  int finishIfUnfinished(UUID id, OffsetDateTime finishedAt, Integer score);
}
