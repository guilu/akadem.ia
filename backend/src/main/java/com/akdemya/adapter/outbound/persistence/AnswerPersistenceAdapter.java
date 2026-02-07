package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.AnswerEntity;
import com.akdemya.adapter.outbound.persistence.mapper.AnswerMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataAnswerRepository;
import com.akdemya.domain.model.Answer;
import com.akdemya.domain.port.out.AnswerRepository;
import org.springframework.stereotype.Component;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AnswerPersistenceAdapter implements AnswerRepository {
  private final SpringDataAnswerRepository repository;
  private final AnswerMapper mapper;
  @PersistenceContext
  private EntityManager entityManager;

  public AnswerPersistenceAdapter(SpringDataAnswerRepository repository, AnswerMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<Answer> findByQuestionId(UUID questionId) {
    return repository.findByQuestion_Id(questionId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Answer> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Answer save(Answer answer) {
    var entity = mapper.toEntity(answer);
    if (answer.getQuestionId() != null) {
      var questionRef = entityManager.getReference(QuestionEntity.class, answer.getQuestionId());
      entity.setQuestion(questionRef);
    }
    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }

  @Override
  public void deleteByQuestionId(UUID questionId) {
    // Assuming repository has this method or we iterate?
    // Domain port has deleteByQuestionId. Spring repo currently has
    // findByQuestionId.
    // We can optimize with custom query or just fetch and delete.
    // Or add deleteByQuestionId to repo.
    // Let's implement fetch and delete for safety/simplicity first, or add to repo.
    // Adding to repo is better.
    // But I need to update SpringDataAnswerRepository first if I want to use it.
    // Or I can use built-in deleteInBatch or deleteAll(items).
    List<AnswerEntity> entities = repository.findByQuestion_Id(questionId);
    repository.deleteAll(entities);
  }
}
