package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.FlashcardJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardRepository;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.port.out.FlashcardRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
  public List<Flashcard> findByUnitId(UUID unitId) {
    return jpa.findByUnitId(unitId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Flashcard save(Flashcard flashcard) {
    return mapper.toDomain(jpa.save(mapper.toEntity(flashcard)));
  }

  @Override
  public void deleteById(UUID id) {
    jpa.deleteById(id);
  }
}
