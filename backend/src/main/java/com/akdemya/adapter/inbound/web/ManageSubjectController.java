package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
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
@RequestMapping("/api/manage/subjects")
public class ManageSubjectController {

  private final ContentManagement contentService;
  private final UserRepository userRepo;

  public ManageSubjectController(ContentManagement contentService, UserRepository userRepo) {
    this.contentService = contentService;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) String scope,
      @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);
    Visibility visibilityScope = parseScope(scope);

    List<Subject> subjects = contentService.getSubjectsByScope(caller.getId(), isAdmin, visibilityScope);
    List<ManageSubjectResponse> result = subjects.stream()
        .map(s -> {
          long unitCount = contentService.getUnitsBySubject(s.getId()).size();
          boolean isEditable = isAdmin || (s.getVisibility() == Visibility.PRIVATE
              && caller.getId().equals(s.getOwnerId()));
          return new ManageSubjectResponse(s.getId(), s.getName(), s.getDescription(),
              unitCount, s.getVisibility(), isEditable);
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody SubjectRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    String visibilityStr = req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE";
    if ("GLOBAL".equals(visibilityStr) && !isAdmin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_subjects"));
    }

    Subject subject;
    if ("GLOBAL".equals(visibilityStr)) {
      subject = Subject.createGlobal(req.name().trim(), req.description());
    } else {
      subject = Subject.createPrivate(req.name().trim(), req.description(), caller.getId());
    }

    Subject saved = contentService.createSubject(subject);
    long unitCount = 0;
    boolean isEditable = isAdmin || saved.getVisibility() == Visibility.PRIVATE;
    return ResponseEntity.ok(new ManageSubjectResponse(saved.getId(), saved.getName(),
        saved.getDescription(), unitCount, saved.getVisibility(), isEditable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id,
                                   @RequestBody SubjectRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean isAdmin = isAdmin(principal);

    Subject current = contentService.getAllSubjects().stream()
        .filter(s -> s.getId().equals(id))
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

    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }

    Subject updated = new Subject(id, name, req.description(), current.getVisibility(), current.getOwnerId());
    Subject saved = contentService.createSubject(updated);
    long unitCount = contentService.getUnitsBySubject(id).size();
    boolean isEditable = isAdmin || (saved.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(saved.getOwnerId()));
    return ResponseEntity.ok(new ManageSubjectResponse(saved.getId(), saved.getName(),
        saved.getDescription(), unitCount, saved.getVisibility(), isEditable));
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

  /** Returns null to mean ALL (GLOBAL + user's PRIVATE), else the explicit scope. */
  private Visibility parseScope(String scope) {
    if ("GLOBAL".equalsIgnoreCase(scope)) return Visibility.GLOBAL;
    if ("PRIVATE".equalsIgnoreCase(scope)) return Visibility.PRIVATE;
    return null; // null = ALL
  }

  public record SubjectRequest(String name, String description, String visibility) {}

  public record ManageSubjectResponse(UUID id, String name, String description,
                                       long unitCount, Visibility visibility, boolean isEditable) {}
}
