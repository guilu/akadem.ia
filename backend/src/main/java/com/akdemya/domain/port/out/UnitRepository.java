package com.akdemya.domain.port.out;

import com.akdemya.domain.model.Unit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
  List<Unit> findBySubjectId(UUID subjectId); // Simplified name, implementation can sort

  Optional<Unit> findById(UUID id);

  Unit save(Unit unit);

  void deleteById(UUID id);
}
