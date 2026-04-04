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
}
