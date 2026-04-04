package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
  List<Question> findByUnitId(UUID unitId);

  org.springframework.data.domain.Page<Question> findPageByUnitId(UUID unitId, int page, int size);

  List<Question> findAll();

  Optional<Question> findById(UUID id);

  Question save(Question question);

  void deleteById(UUID id);

  long countByUnitId(UUID unitId);

  long countByUnitIdAndDifficulty(UUID unitId, String difficulty);

  /** Returns all GLOBAL questions plus the caller's own PRIVATE questions. */
  List<Question> findVisibleByUserId(UUID userId);

  /** Returns GLOBAL questions plus the caller's own PRIVATE questions for the given unit. */
  List<Question> findVisibleByUnitIdAndUserId(UUID unitId, UUID userId);
}
