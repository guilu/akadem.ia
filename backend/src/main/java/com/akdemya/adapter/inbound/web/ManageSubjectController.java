package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/manage/subjects")
public class ManageSubjectController {

  private final ContentManagement contentService;
  private final UserRepository userRepo;

  public ManageSubjectController(ContentManagement contentService, UserRepository userRepo) {
    this.contentService = contentService;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<List<ManageSubjectResponse>> list(@AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    List<Subject> subjects = contentService.getSubjectsByScope(
        caller.getId(), isAdmin, isAdmin ? Visibility.GLOBAL : Visibility.PRIVATE);
    List<ManageSubjectResponse> result = subjects.stream()
        .map(s -> {
          long unitCount = contentService.getUnitsBySubject(s.getId()).size();
          return new ManageSubjectResponse(s.getId(), s.getName(), s.getDescription(),
              unitCount, s.getVisibility(), isManageEditable(s, caller, isAdmin), s.getSyllabusId());
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<Object> create(@Valid @RequestBody SubjectRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    String visibilityStr = isAdmin ? "GLOBAL" : (req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE");
    if ("GLOBAL".equals(visibilityStr) && !isAdmin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_subjects"));
    }

    Subject subject;
    if ("GLOBAL".equals(visibilityStr)) {
      subject = Subject.createGlobal(req.name().trim(), req.description(), req.syllabusId());
    } else {
      subject = Subject.createPrivate(req.name().trim(), req.description(), caller.getId(), req.syllabusId());
    }

    Subject saved = contentService.createSubject(subject);
    long unitCount = 0;
    return ResponseEntity.ok(new ManageSubjectResponse(saved.getId(), saved.getName(),
        saved.getDescription(), unitCount, saved.getVisibility(), isManageEditable(saved, caller, isAdmin), saved.getSyllabusId()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Object> update(@PathVariable UUID id,
                                   @Valid @RequestBody SubjectRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    Subject current = contentService.getSubjectById(id).orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }

    boolean canEdit = isManageEditable(current, caller, isAdmin);
    if (!canEdit) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }

    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }

    UUID syllabusId = req.syllabusId() != null ? req.syllabusId() : current.getSyllabusId();
    Subject updated = new Subject(id, name, req.description(), current.getVisibility(), current.getOwnerId(), syllabusId);
    Subject saved = contentService.createSubject(updated);
    long unitCount = contentService.getUnitsBySubject(id).size();
    return ResponseEntity.ok(new ManageSubjectResponse(saved.getId(), saved.getName(),
        saved.getDescription(), unitCount, saved.getVisibility(), isManageEditable(saved, caller, isAdmin), saved.getSyllabusId()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> delete(@PathVariable UUID id,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    Subject current = contentService.getSubjectById(id).orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }
    if (!isManageEditable(current, caller, isAdmin)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }
    try {
      contentService.deleteSubjectIfAuthorized(id, caller.getId(), isAdmin);
      return ResponseEntity.ok().build();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private AppUser resolveUser(User principal) {
    return userRepo.findByEmail(principal.getUsername())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            HttpStatus.UNAUTHORIZED));
  }

  private boolean isAdmin(User principal) {
    return principal.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  private boolean isManageEditable(Subject subject, AppUser caller, boolean isAdmin) {
    if (isAdmin && subject.getVisibility() == Visibility.GLOBAL) return true;
    return subject.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(subject.getOwnerId());
  }

  private static final long MAX_UPLOAD_SIZE = 10L * 1024 * 1024;
  private static final int MAX_IMPORT_ROWS = 500;

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<Object> importSubjects(
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "csv") String format,
      @RequestParam UUID syllabusId,
      @AuthenticationPrincipal User principal) throws Exception {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "file_required"));
    }
    if (file.getSize() > MAX_UPLOAD_SIZE) {
      return ResponseEntity.badRequest().body(Map.of("error", "file_too_large"));
    }
    if (syllabusId == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "syllabusId_required"));
    }

    // Ownership check: non-admins can only import into syllabuses they own
    if (!isAdmin) {
      Syllabus syllabus = contentService.getSyllabusById(syllabusId).orElse(null);
      if (syllabus == null || !caller.getId().equals(syllabus.getOwnerId())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "not_authorized"));
      }
    }

    // Load existing subject names once (no N+1) for duplicate detection
    Visibility importVisibility = isAdmin ? Visibility.GLOBAL : Visibility.PRIVATE;
    UUID importOwner = isAdmin ? null : caller.getId();
    List<Subject> existing = contentService.getSubjectsByScope(caller.getId(), isAdmin, importVisibility);
    Set<String> existingNames = new java.util.HashSet<>();
    for (Subject s : existing) {
      if (syllabusId.equals(s.getSyllabusId())) {
        existingNames.add(s.getName().trim().toLowerCase());
      }
    }

    int created = 0;
    List<Map<String, Object>> errors = new ArrayList<>();

    if (format.equalsIgnoreCase("csv")) {
      String content = new String(file.getBytes());
      List<String[]> rows = parseCsv(content);
      // rows.get(0) is header; data rows start at index 1
      int dataRowCount = rows.size() - 1;
      if (dataRowCount > MAX_IMPORT_ROWS) {
        return ResponseEntity.badRequest().body(Map.of("error", "row_limit_exceeded"));
      }
      for (int i = 1; i < rows.size(); i++) {
        int rowNum = i; // 1-based data row number
        String[] row = rows.get(i);
        String name = row.length > 0 ? row[0].trim() : "";
        String description = row.length > 1 ? row[1].trim() : null;
        if (description != null && description.isBlank()) description = null;

        if (name.isEmpty()) {
          errors.add(Map.of("row", rowNum, "message", "name_required"));
          continue;
        }
        if (existingNames.contains(name.toLowerCase())) {
          errors.add(Map.of("row", rowNum, "message", "duplicate_name"));
          continue;
        }
        try {
          Subject subject = isAdmin
              ? Subject.createGlobal(name, description, syllabusId)
              : Subject.createPrivate(name, description, importOwner, syllabusId);
          contentService.createSubject(subject);
          existingNames.add(name.toLowerCase());
          created++;
        } catch (Exception e) {
          errors.add(Map.of("row", rowNum, "message", e.getMessage() != null ? e.getMessage() : "import_error"));
        }
      }
    } else {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      ImportSubjectRow[] rows = mapper.readValue(file.getBytes(), ImportSubjectRow[].class);
      if (rows.length > MAX_IMPORT_ROWS) {
        return ResponseEntity.badRequest().body(Map.of("error", "row_limit_exceeded"));
      }
      for (int i = 0; i < rows.length; i++) {
        int rowNum = i + 1;
        ImportSubjectRow row = rows[i];
        String name = row.name() != null ? row.name().trim() : "";
        String description = row.description() != null && !row.description().isBlank() ? row.description().trim() : null;

        if (name.isEmpty()) {
          errors.add(Map.of("row", rowNum, "message", "name_required"));
          continue;
        }
        if (existingNames.contains(name.toLowerCase())) {
          errors.add(Map.of("row", rowNum, "message", "duplicate_name"));
          continue;
        }
        try {
          Subject subject = isAdmin
              ? Subject.createGlobal(name, description, syllabusId)
              : Subject.createPrivate(name, description, importOwner, syllabusId);
          contentService.createSubject(subject);
          existingNames.add(name.toLowerCase());
          created++;
        } catch (Exception e) {
          errors.add(Map.of("row", rowNum, "message", e.getMessage() != null ? e.getMessage() : "import_error"));
        }
      }
    }

    return ResponseEntity.ok(Map.of("created", created, "errors", errors));
  }

  private List<String[]> parseCsv(String content) {
    List<String[]> rows = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    List<String> row = new ArrayList<>();
    boolean inQuotes = false;
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '"') {
        if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
          current.append('"'); i++;
        } else { inQuotes = !inQuotes; }
      } else if (c == ',' && !inQuotes) {
        row.add(current.toString()); current.setLength(0);
      } else if ((c == '\n' || c == '\r') && !inQuotes) {
        if (current.length() > 0 || !row.isEmpty()) {
          row.add(current.toString());
          rows.add(row.toArray(new String[0]));
          row = new ArrayList<>();
          current.setLength(0);
        }
      } else { current.append(c); }
    }
    if (current.length() > 0 || !row.isEmpty()) {
      row.add(current.toString()); rows.add(row.toArray(new String[0]));
    }
    return rows;
  }

  record ImportSubjectRow(String name, String description) {}

  public record SubjectRequest(String name, String description, String visibility, UUID syllabusId) {}

  public record ManageSubjectResponse(UUID id, String name, String description,
                                       long unitCount, Visibility visibility, boolean isEditable, UUID syllabusId) {}
}
