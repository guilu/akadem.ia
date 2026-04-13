package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Syllabus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusRepository {
  List<Syllabus> findAll();

  Optional<Syllabus> findById(UUID id);

  Syllabus save(Syllabus syllabus);

  void deleteById(UUID id);

  /** Returns all GLOBAL syllabuses plus the caller's own PRIVATE syllabuses. */
  List<Syllabus> findVisibleByUserId(UUID userId);
}
