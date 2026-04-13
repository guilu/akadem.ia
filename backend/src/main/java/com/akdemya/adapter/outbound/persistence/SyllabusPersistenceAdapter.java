package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.SyllabusMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSyllabusRepository;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.port.out.SyllabusRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SyllabusPersistenceAdapter implements SyllabusRepository {
  private final SpringDataSyllabusRepository repository;
  private final SyllabusMapper mapper;

  public SyllabusPersistenceAdapter(SpringDataSyllabusRepository repository, SyllabusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<Syllabus> findAll() {
    return repository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Syllabus> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Syllabus save(Syllabus syllabus) {
    return mapper.toDomain(repository.save(mapper.toEntity(syllabus)));
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }

  @Override
  public List<Syllabus> findVisibleByUserId(UUID userId) {
    return repository.findByVisibilityOrOwnerId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
