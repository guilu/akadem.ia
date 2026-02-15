package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.ExamAttemptEntity;
import com.akdemya.adapter.outbound.persistence.mapper.ExamAttemptMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataExamAttemptRepository;
import com.akdemya.domain.model.ExamAttempt;
import com.akdemya.domain.port.out.ExamAttemptRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ExamAttemptPersistenceAdapter implements ExamAttemptRepository {
  private final SpringDataExamAttemptRepository repository;
  private final ExamAttemptMapper mapper;

  public ExamAttemptPersistenceAdapter(SpringDataExamAttemptRepository repository, ExamAttemptMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public ExamAttempt save(ExamAttempt attempt) {
    ExamAttemptEntity entity = mapper.toEntity(attempt);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<ExamAttempt> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<ExamAttempt> findByUserEmail(String userEmail) {
    return repository.findByUserEmailOrderByStartedAtDesc(userEmail).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
