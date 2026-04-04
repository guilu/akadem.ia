package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/subjects")
public class AdminSubjectController {
  private final SubjectRepository subjects;
  private final UnitRepository units;

  public AdminSubjectController(SubjectRepository subjects, UnitRepository units) {
    this.subjects = subjects;
    this.units = units;
  }

  @GetMapping
  public List<SubjectResponse> list() {
    return subjects.findAll().stream()
        .map(s -> SubjectResponse.from(s, units.findBySubjectId(s.getId()).size()))
        .toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody SubjectRequest req) {
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Subject subject = Subject.createGlobal(req.name().trim(), req.description());
    return ResponseEntity.ok(SubjectResponse.from(subjects.save(subject), 0));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody SubjectRequest req) {
    Subject current = subjects.findById(id).orElse(null);
    if (current == null) return ResponseEntity.notFound().build();
    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Subject updated = new Subject(id, name, req.description(), current.getVisibility(), current.getOwnerId());
    int unitCount = units.findBySubjectId(id).size();
    return ResponseEntity.ok(SubjectResponse.from(subjects.save(updated), unitCount));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    subjects.deleteById(id);
    return ResponseEntity.ok().build();
  }

  record SubjectRequest(String name, String description) {}
  record SubjectResponse(UUID id, String name, String description, int unitCount, Visibility visibility) {
    static SubjectResponse from(Subject s, int unitCount) {
      return new SubjectResponse(s.getId(), s.getName(), s.getDescription(), unitCount, s.getVisibility());
    }
  }
}
