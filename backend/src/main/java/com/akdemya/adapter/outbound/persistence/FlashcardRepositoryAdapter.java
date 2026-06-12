package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.FlashcardJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardRepository;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.port.out.FlashcardRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FlashcardRepositoryAdapter implements FlashcardRepository {

  private final JpaFlashcardRepository jpa;
  private final FlashcardJpaMapper mapper;

  public FlashcardRepositoryAdapter(JpaFlashcardRepository jpa, FlashcardJpaMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public Optional<Flashcard> findById(UUID id) {
    return jpa.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Flashcard> findByIdForUpdate(UUID id) {
    return jpa.findByIdForUpdate(id).map(mapper::toDomain);
  }

  @Override
  public List<Flashcard> findByUnitId(UUID unitId) {
    return jpa.findByUnitId(unitId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Flashcard> findByIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return jpa.findByIdIn(ids).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Flashcard> findNewByUserIdAndUnitId(UUID userId, UUID unitId, int limit) {
    int pageSize = Math.max(0, limit);
    if (pageSize == 0) return List.of();
    return jpa.findNewByUserIdAndUnitId(userId, unitId, PageRequest.of(0, pageSize))
        .stream().map(mapper::toDomain).toList();
  }

  @Override
  public long countNewByUserIdAndUnitId(UUID userId, UUID unitId) {
    return jpa.countNewByUserIdAndUnitId(userId, unitId);
  }

  @Override
  public long countNewByUserId(UUID userId) {
    return jpa.countNewByUserId(userId);
  }

  @Override
  public Flashcard save(Flashcard flashcard) {
    return mapper.toDomain(jpa.save(mapper.toEntity(flashcard)));
  }

  @Override
  public void deleteById(UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public List<Flashcard> findVisibleByUserId(UUID userId) {
    return jpa.findVisibleByUserId(userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Flashcard> findVisibleByUnitIdAndUserId(UUID unitId, UUID userId) {
    return jpa.findVisibleByUnitIdAndUserId(unitId, userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Map<UUID, Long> countNewByUserIdGroupByUnit(UUID userId) {
    return jpa.countNewByUserIdGroupByUnit(userId)
        .stream()
        .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
  }
}
