package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Flashcard;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository {

  Optional<Flashcard> findById(UUID id);

  List<Flashcard> findByUnitId(UUID unitId);

  List<Flashcard> findByIds(List<UUID> ids);

  List<Flashcard> findNewByUserIdAndUnitId(UUID userId, UUID unitId, int limit);

  long countNewByUserIdAndUnitId(UUID userId, UUID unitId);

  long countNewByUserId(UUID userId);

  /** Returns the count of new (unreviewed) flashcards per unitId for the given user. */
  Map<UUID, Long> countNewByUserIdGroupByUnit(UUID userId);

  Flashcard save(Flashcard flashcard);

  void deleteById(UUID id);

  /** Returns all GLOBAL flashcards plus the caller's own PRIVATE flashcards. */
  List<Flashcard> findVisibleByUserId(UUID userId);

  /** Returns GLOBAL flashcards plus the caller's own PRIVATE flashcards for the given unit. */
  List<Flashcard> findVisibleByUnitIdAndUserId(UUID unitId, UUID userId);
}
