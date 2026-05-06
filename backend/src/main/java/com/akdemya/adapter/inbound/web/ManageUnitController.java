package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Unit;
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
@RequestMapping("/api/manage/units")
public class ManageUnitController {

  private final ContentManagement contentService;
  private final UserRepository userRepo;

  public ManageUnitController(ContentManagement contentService, UserRepository userRepo) {
    this.contentService = contentService;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam UUID subjectId,
      @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    List<Unit> units = contentService.getUnitsByScope(
        subjectId, caller.getId(), isAdmin, isAdmin ? Visibility.GLOBAL : Visibility.PRIVATE);
    List<ManageUnitResponse> result = units.stream()
        .map(u -> {
          long questionCount = contentService.getQuestionsByUnit(u.getId()).size();
          return new ManageUnitResponse(u.getId(), u.getSubjectId(), u.getName(),
              u.getDescription(), u.getOrderIndex(), questionCount, u.getVisibility(), isManageEditable(u, caller, isAdmin));
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody UnitRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.subjectId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_required"));
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }

    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    String visibilityStr = isAdmin ? "GLOBAL" : (req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE");

    if ("GLOBAL".equals(visibilityStr) && !isAdmin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_units"));
    }

    Unit unit;
    if ("GLOBAL".equals(visibilityStr)) {
      unit = Unit.createGlobal(req.subjectId(), req.name().trim(), req.description(), req.orderIndex());
    } else {
      unit = Unit.createPrivate(req.subjectId(), req.name().trim(), req.description(), req.orderIndex(), caller.getId());
    }

    try {
      Unit saved = contentService.createUnit(unit);
      long questionCount = 0;
      return ResponseEntity.ok(new ManageUnitResponse(saved.getId(), saved.getSubjectId(),
          saved.getName(), saved.getDescription(), saved.getOrderIndex(), questionCount,
          saved.getVisibility(), isManageEditable(saved, caller, isAdmin)));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id,
                                   @Valid @RequestBody UnitRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    if (req.subjectId() == null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "subject_required"));
    }
    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }

    // Find unit by looking through the subject's units
    Unit current = contentService.getUnitsBySubject(req.subjectId()).stream()
        .filter(u -> u.getId().equals(id))
        .findFirst().orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }

    boolean canEdit = isManageEditable(current, caller, isAdmin);
    if (!canEdit) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }

    Unit updated = new Unit(id, req.subjectId(), name, req.description(),
        req.orderIndex(), current.getVisibility(), current.getOwnerId());
    try {
      Unit saved = contentService.createUnit(updated);
      long questionCount = contentService.getQuestionsByUnit(id).size();
      return ResponseEntity.ok(new ManageUnitResponse(saved.getId(), saved.getSubjectId(),
          saved.getName(), saved.getDescription(), saved.getOrderIndex(), questionCount,
          saved.getVisibility(), isManageEditable(saved, caller, isAdmin)));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    Unit current = contentService.getUnitById(id).orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }
    if (!isManageEditable(current, caller, isAdmin)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }
    try {
      contentService.deleteUnitIfAuthorized(id, caller.getId(), isAdmin);
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

  private boolean isManageEditable(Unit unit, AppUser caller, boolean isAdmin) {
    if (isAdmin && unit.getVisibility() == Visibility.GLOBAL) return true;
    return unit.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(unit.getOwnerId());
  }

  private static final long MAX_UPLOAD_SIZE = 10L * 1024 * 1024;
  private static final int MAX_IMPORT_ROWS = 500;

  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public ResponseEntity<Object> importUnits(
      @RequestParam("file") MultipartFile file,
      @RequestParam(defaultValue = "csv") String format,
      @RequestParam UUID subjectId,
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
    if (subjectId == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "subjectId_required"));
    }

    // Ownership check: non-admins can only import into subjects they own
    if (!isAdmin) {
      Subject subject = contentService.getSubjectById(subjectId).orElse(null);
      if (subject == null || !caller.getId().equals(subject.getOwnerId())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "not_authorized"));
      }
    }

    // Load existing unit names once (no N+1) for duplicate detection
    List<Unit> existing = contentService.getUnitsBySubject(subjectId);
    Set<String> existingNames = new java.util.HashSet<>();
    for (Unit u : existing) {
      existingNames.add(u.getName().trim().toLowerCase());
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
          int orderIndex = created + existing.size() + 1;
          Unit unit = isAdmin
              ? Unit.createGlobal(subjectId, name, description, orderIndex)
              : Unit.createPrivate(subjectId, name, description, orderIndex, caller.getId());
          contentService.createUnit(unit);
          existingNames.add(name.toLowerCase());
          created++;
        } catch (Exception e) {
          errors.add(Map.of("row", rowNum, "message", e.getMessage() != null ? e.getMessage() : "import_error"));
        }
      }
    } else {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      ImportUnitRow[] rows = mapper.readValue(file.getBytes(), ImportUnitRow[].class);
      if (rows.length > MAX_IMPORT_ROWS) {
        return ResponseEntity.badRequest().body(Map.of("error", "row_limit_exceeded"));
      }
      for (int i = 0; i < rows.length; i++) {
        int rowNum = i + 1;
        ImportUnitRow row = rows[i];
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
          int orderIndex = created + existing.size() + 1;
          Unit unit = isAdmin
              ? Unit.createGlobal(subjectId, name, description, orderIndex)
              : Unit.createPrivate(subjectId, name, description, orderIndex, caller.getId());
          contentService.createUnit(unit);
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

  record ImportUnitRow(String name, String description) {}

  public record UnitRequest(UUID subjectId, String name, String description,
                              int orderIndex, String visibility) {}

  public record ManageUnitResponse(UUID id, UUID subjectId, String name, String description,
                                    int orderIndex, long questionCount,
                                    Visibility visibility, boolean isEditable) {}
}
