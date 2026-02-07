package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.adapter.outbound.persistence.mapper.SubjectMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSubjectRepository;
import com.akdemya.domain.model.Subject;
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
}
