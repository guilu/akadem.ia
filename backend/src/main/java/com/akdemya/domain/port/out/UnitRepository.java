package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Unit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
  List<Unit> findBySubjectId(UUID subjectId); // Simplified name, implementation can sort

  List<Unit> findBySubjectIdWithFlashcards(UUID subjectId);

  List<Unit> findAll();

  List<Unit> findAllWithFlashcards();

  Optional<Unit> findById(UUID id);

  Unit save(Unit unit);

  void deleteById(UUID id);

  /** Returns all GLOBAL units plus the caller's own PRIVATE units for the given subject. */
  List<Unit> findVisibleBySubjectIdAndUserId(UUID subjectId, UUID userId);

  /**
   * Scope-aware query for a given subject:
   * - GLOBAL → visibility=GLOBAL
   * - PRIVATE → visibility=PRIVATE AND ownerId=userId
   * - ALL → visibility=GLOBAL OR (visibility=PRIVATE AND ownerId=userId)
   */
  List<Unit> findBySubjectIdAndScope(UUID subjectId, UUID userId, com.akdemya.domain.model.Visibility scope);
}
