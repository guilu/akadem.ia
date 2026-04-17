package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
  List<Subject> findAll();

  Optional<Subject> findById(UUID id);

  Subject save(Subject subject);

  void deleteById(UUID id);

  /** Returns all GLOBAL subjects plus the caller's own PRIVATE subjects. */
  List<Subject> findVisibleByUserId(UUID userId);

  /**
   * Scope-aware query:
   * - GLOBAL → visibility=GLOBAL
   * - PRIVATE → visibility=PRIVATE AND ownerId=userId
   * - ALL → visibility=GLOBAL OR (visibility=PRIVATE AND ownerId=userId)
   */
  List<Subject> findByScope(UUID userId, Visibility scope);

  /** Returns all subjects belonging to a given syllabus. */
  List<Subject> findBySyllabusId(UUID syllabusId);

  /** Returns visible subjects (GLOBAL + caller's PRIVATE) for a given syllabus. */
  List<Subject> findVisibleBySyllabusId(UUID syllabusId, UUID userId);
}
