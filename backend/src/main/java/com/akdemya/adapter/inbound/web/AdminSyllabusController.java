package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.SyllabusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/syllabuses")
public class AdminSyllabusController {

  private final SyllabusRepository syllabusRepo;

  public AdminSyllabusController(SyllabusRepository syllabusRepo) {
    this.syllabusRepo = syllabusRepo;
  }

  @GetMapping
  public List<SyllabusResponse> list() {
    return syllabusRepo.findAll().stream()
        .map(SyllabusResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody SyllabusRequest req) {
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Syllabus syllabus = Syllabus.createGlobal(req.name().trim(), req.description());
    return ResponseEntity.ok(SyllabusResponse.from(syllabusRepo.save(syllabus)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody SyllabusRequest req) {
    Syllabus current = syllabusRepo.findById(id).orElse(null);
    if (current == null) return ResponseEntity.notFound().build();
    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    Syllabus updated = new Syllabus(id, name, req.description(), current.getVisibility(), current.getOwnerId());
    return ResponseEntity.ok(SyllabusResponse.from(syllabusRepo.save(updated)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id) {
    syllabusRepo.deleteById(id);
    return ResponseEntity.ok().build();
  }

  record SyllabusRequest(String name, String description) {}

  record SyllabusResponse(UUID id, String name, String description, Visibility visibility, UUID ownerId) {
    static SyllabusResponse from(Syllabus s) {
      return new SyllabusResponse(s.getId(), s.getName(), s.getDescription(), s.getVisibility(), s.getOwnerId());
    }
  }
}
