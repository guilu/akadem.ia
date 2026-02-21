package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Question;
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
}
