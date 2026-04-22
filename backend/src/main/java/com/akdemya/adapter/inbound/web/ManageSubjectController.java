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
import jakarta.validation.Valid;
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

  public record SubjectRequest(String name, String description, String visibility, UUID syllabusId) {}

  public record ManageSubjectResponse(UUID id, String name, String description,
                                       long unitCount, Visibility visibility, boolean isEditable, UUID syllabusId) {}
}
