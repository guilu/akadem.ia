package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.out.QuestionRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/units")
public class AdminUnitController {
  private final UnitRepository units;
  private final SubjectRepository subjects;
  private final QuestionRepository questions;

  public AdminUnitController(UnitRepository units, SubjectRepository subjects, QuestionRepository questions) {
    this.units = units;
    this.subjects = subjects;
    this.questions = questions;
  }

  @GetMapping
  public List<UnitResponse> list(@RequestParam UUID subjectId) {
    return units.findBySubjectId(subjectId).stream()
        .map(u -> UnitResponse.from(u, questions.countByUnitId(u.getId())))
        .toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody UnitRequest req) {
    if (req.subjectId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_required"));
    }
    if (subjects.findById(req.subjectId()).isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_not_found"));
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Unit unit = Unit.create(req.subjectId(), req.name().trim(), req.description(), req.orderIndex());
    return ResponseEntity.ok(UnitResponse.from(units.save(unit), 0));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UnitRequest req) {
    Unit current = units.findById(id).orElse(null);
    if (current == null) return ResponseEntity.notFound().build();
    if (req.subjectId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_required"));
    }
    if (subjects.findById(req.subjectId()).isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_not_found"));
    }
    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Unit updated = new Unit(id, req.subjectId(), name, req.description(), req.orderIndex());
    long questionCount = questions.countByUnitId(id);
    return ResponseEntity.ok(UnitResponse.from(units.save(updated), questionCount));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    units.deleteById(id);
    return ResponseEntity.ok().build();
  }

  record UnitRequest(UUID subjectId, String name, String description, int orderIndex) {}
  record UnitResponse(UUID id, UUID subjectId, String name, String description, int orderIndex, long questionCount) {
    static UnitResponse from(Unit u, long questionCount) {
      return new UnitResponse(u.getId(), u.getSubjectId(), u.getName(), u.getDescription(), u.getOrderIndex(), questionCount);
    }
  }
}
