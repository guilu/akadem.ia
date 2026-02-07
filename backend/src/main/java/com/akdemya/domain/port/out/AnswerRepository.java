package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Answer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository {
  List<Answer> findByQuestionId(UUID questionId);

  Optional<Answer> findById(UUID id);

  Answer save(Answer answer);

  void deleteById(UUID id);

  void deleteByQuestionId(UUID questionId);
}
