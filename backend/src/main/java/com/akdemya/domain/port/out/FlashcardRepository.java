package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Flashcard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository {

  Optional<Flashcard> findById(UUID id);

  List<Flashcard> findByUnitId(UUID unitId);

  Flashcard save(Flashcard flashcard);

  void deleteById(UUID id);
}
