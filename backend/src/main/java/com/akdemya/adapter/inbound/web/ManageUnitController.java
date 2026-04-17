package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    List<Unit> units = contentService.getUnitsByScope(subjectId, caller.getId(), false, Visibility.PRIVATE);
    List<ManageUnitResponse> result = units.stream()
        .map(u -> {
          long questionCount = contentService.getQuestionsByUnit(u.getId()).size();
          return new ManageUnitResponse(u.getId(), u.getSubjectId(), u.getName(),
              u.getDescription(), u.getOrderIndex(), questionCount, u.getVisibility(), true);
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody UnitRequest req,
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
    String visibilityStr = req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE";

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
      boolean isEditable = isAdmin || saved.getVisibility() == Visibility.PRIVATE;
      return ResponseEntity.ok(new ManageUnitResponse(saved.getId(), saved.getSubjectId(),
          saved.getName(), saved.getDescription(), saved.getOrderIndex(), questionCount,
          saved.getVisibility(), isEditable));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id,
                                   @RequestBody UnitRequest req,
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

    boolean canEdit = isAdmin || (current.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(current.getOwnerId()));
    if (!canEdit) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }

    Unit updated = new Unit(id, req.subjectId(), name, req.description(),
        req.orderIndex(), current.getVisibility(), current.getOwnerId());
    try {
      Unit saved = contentService.createUnit(updated);
      long questionCount = contentService.getQuestionsByUnit(id).size();
      boolean isEditable = isAdmin || (saved.getVisibility() == Visibility.PRIVATE
          && caller.getId().equals(saved.getOwnerId()));
      return ResponseEntity.ok(new ManageUnitResponse(saved.getId(), saved.getSubjectId(),
          saved.getName(), saved.getDescription(), saved.getOrderIndex(), questionCount,
          saved.getVisibility(), isEditable));
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

  public record UnitRequest(UUID subjectId, String name, String description,
                              int orderIndex, String visibility) {}

  public record ManageUnitResponse(UUID id, UUID subjectId, String name, String description,
                                    int orderIndex, long questionCount,
                                    Visibility visibility, boolean isEditable) {}
}
