package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptAnswerEntity;
import com.akdemya.adapter.outbound.persistence.mapper.ExamAttemptAnswerMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataExamAttemptAnswerRepository;
import com.akdemya.domain.model.ExamAttemptAnswer;
import com.akdemya.domain.port.out.ExamAttemptAnswerRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ExamAttemptAnswerPersistenceAdapter implements ExamAttemptAnswerRepository {
  private final SpringDataExamAttemptAnswerRepository repository;
  private final ExamAttemptAnswerMapper mapper;

  public ExamAttemptAnswerPersistenceAdapter(SpringDataExamAttemptAnswerRepository repository,
      ExamAttemptAnswerMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public ExamAttemptAnswer save(ExamAttemptAnswer answer) {
    ExamAttemptAnswerEntity entity = mapper.toEntity(answer);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public void saveAll(List<ExamAttemptAnswer> answers) {
    List<ExamAttemptAnswerEntity> entities = answers.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toList());
    repository.saveAll(entities);
  }

  @Override
  public List<ExamAttemptAnswer> findByAttemptId(UUID attemptId) {
    return repository.findByAttemptId(attemptId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public java.util.Optional<ExamAttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId) {
    return repository.findByAttemptIdAndQuestionId(attemptId, questionId).map(mapper::toDomain);
  }
}
