package com.akdemya.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.akdemya.adapter.outbound.persistence.mapper.QuestionMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataQuestionRepository;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.port.out.QuestionRepository;

@Component
public class QuestionPersistenceAdapter implements QuestionRepository {
  private final SpringDataQuestionRepository repository;
  private final QuestionMapper mapper;
  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManager;

  public QuestionPersistenceAdapter(SpringDataQuestionRepository repository, QuestionMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<Question> findByUnitId(UUID unitId) {
    return repository.findByUnit_Id(unitId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public org.springframework.data.domain.Page<Question> findPageByUnitId(UUID unitId, int page, int size) {
    return repository.findByUnit_Id(unitId, org.springframework.data.domain.PageRequest.of(page, size))
        .map(mapper::toDomain);
  }

  @Override
  public Optional<Question> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Question> findAll() {
    return repository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Question save(Question question) {
    var entity = mapper.toEntity(question);
    if (question.getUnitId() != null) {
      var unitRef = entityManager.getReference(com.akdemya.adapter.outbound.persistence.entity.UnitEntity.class,
          question.getUnitId());
      entity.setUnit(unitRef);
    }
    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }

  @Override
  public long countByUnitId(UUID unitId) {
    return repository.countByUnit_Id(unitId);
  }

  @Override
  public long countByUnitIdAndDifficulty(UUID unitId, Question.Difficulty difficulty) {
    return repository.countByUnit_IdAndDifficulty(unitId, difficulty);
  }
}
