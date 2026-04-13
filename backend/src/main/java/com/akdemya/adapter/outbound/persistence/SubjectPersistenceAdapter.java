package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.adapter.outbound.persistence.mapper.SubjectMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSubjectRepository;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.SubjectRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SubjectPersistenceAdapter implements SubjectRepository {
  private final SpringDataSubjectRepository repository;
  private final SubjectMapper mapper;

  public SubjectPersistenceAdapter(SpringDataSubjectRepository repository, SubjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<Subject> findAll() {
    return repository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Subject> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Subject save(Subject subject) {
    SubjectEntity entity = mapper.toEntity(subject);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }

  @Override
  public List<Subject> findVisibleByUserId(UUID userId) {
    return repository.findVisibleByUserId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Subject> findByScope(UUID userId, Visibility scope) {
    if (scope == null) {
      // null = ALL: GLOBAL + user's PRIVATE
      return repository.findAllByScope(userId).stream()
          .map(mapper::toDomain).collect(Collectors.toList());
    }
    List<SubjectEntity> entities = switch (scope) {
      case GLOBAL -> repository.findAllGlobal();
      case PRIVATE -> repository.findPrivateByOwner(userId);
    };
    return entities.stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<Subject> findBySyllabusId(UUID syllabusId) {
    return repository.findBySyllabusId(syllabusId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Subject> findVisibleBySyllabusId(UUID syllabusId, UUID userId) {
    return repository.findVisibleBySyllabusId(syllabusId, userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
