package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/subjects")
@CrossOrigin(origins = "*")
public class AdminSubjectController {
  private final SubjectRepository subjects;
  private final UnitRepository units;

  public AdminSubjectController(SubjectRepository subjects, UnitRepository units) {
    this.subjects = subjects;
    this.units = units;
  }

  @GetMapping
  public List<SubjectResponse> list() {
    return subjects.findAll().stream().map(SubjectResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody SubjectRequest req) {
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Subject subject = Subject.create(req.name().trim(), req.description());
    return ResponseEntity.ok(SubjectResponse.from(subjects.save(subject)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody SubjectRequest req) {
    Subject current = subjects.findById(id).orElse(null);
    if (current == null) return ResponseEntity.notFound().build();
    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Subject updated = new Subject(id, name, req.description());
    return ResponseEntity.ok(SubjectResponse.from(subjects.save(updated)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    if (!units.findBySubjectId(id).isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_has_units"));
    }
    subjects.deleteById(id);
    return ResponseEntity.ok().build();
  }

  record SubjectRequest(String name, String description) {}
  record SubjectResponse(UUID id, String name, String description) {
    static SubjectResponse from(Subject s) {
      return new SubjectResponse(s.getId(), s.getName(), s.getDescription());
    }
  }
}
