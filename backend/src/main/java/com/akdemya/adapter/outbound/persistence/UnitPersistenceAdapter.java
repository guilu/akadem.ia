package com.akdemya.adapter.outbound.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.akdemya.adapter.outbound.persistence.mapper.UnitMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataUnitRepository;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.out.UnitRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class UnitPersistenceAdapter implements UnitRepository {
  private final SpringDataUnitRepository repository;
  private final UnitMapper mapper;
  @PersistenceContext
  private EntityManager entityManager;

  public UnitPersistenceAdapter(SpringDataUnitRepository repository, UnitMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<Unit> findBySubjectId(UUID subjectId) {
    return repository.findBySubject_IdOrderByOrderIndexAscNameAsc(subjectId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Unit> findAll() {
    return repository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Unit> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Unit save(Unit unit) {
    var entity = mapper.toEntity(unit);
    if (unit.getSubjectId() != null) {
      var subjectRef = entityManager.getReference(com.akdemya.adapter.outbound.persistence.entity.SubjectEntity.class,
          unit.getSubjectId());
      entity.setSubject(subjectRef);
    }
    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
