package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Syllabus;
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
@RequestMapping("/api/manage/syllabuses")
public class ManageSyllabusController {

  private final ContentManagement contentService;
  private final UserRepository userRepo;

  public ManageSyllabusController(ContentManagement contentService, UserRepository userRepo) {
    this.contentService = contentService;
    this.userRepo = userRepo;
  }

  @GetMapping
  public ResponseEntity<?> list(@AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean admin = isAdmin(principal);
    List<ManageSyllabusResponse> result = contentService.getVisibleSyllabuses(caller.getId()).stream()
        .map(s -> {
          boolean isEditable = admin || (s.getVisibility() == Visibility.PRIVATE
              && caller.getId().equals(s.getOwnerId()));
          return new ManageSyllabusResponse(s.getId(), s.getName(), s.getDescription(),
              s.getVisibility() != null ? s.getVisibility().name() : null, isEditable);
        })
        .toList();
    return ResponseEntity.ok(result);
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody SyllabusRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    AppUser caller = resolveUser(principal);
    boolean admin = isAdmin(principal);

    String visibilityStr = req.visibility() != null ? req.visibility().toUpperCase() : "PRIVATE";
    if ("GLOBAL".equals(visibilityStr) && !admin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_syllabuses"));
    }

    Syllabus syllabus;
    if ("GLOBAL".equals(visibilityStr)) {
      syllabus = Syllabus.createGlobal(req.name().trim(), req.description());
    } else {
      syllabus = Syllabus.createPrivate(req.name().trim(), req.description(), caller.getId());
    }

    Syllabus saved = contentService.createSyllabus(syllabus);
    boolean isEditable = admin || saved.getVisibility() == Visibility.PRIVATE;
    return ResponseEntity.ok(new ManageSyllabusResponse(saved.getId(), saved.getName(),
        saved.getDescription(), saved.getVisibility() != null ? saved.getVisibility().name() : null, isEditable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable UUID id,
                                   @RequestBody SyllabusRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean admin = isAdmin(principal);

    Syllabus current = contentService.getSyllabusById(id).orElse(null);
    if (current == null) {
      return ResponseEntity.notFound().build();
    }

    boolean canEdit = admin || (current.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(current.getOwnerId()));
    if (!canEdit) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "not_authorized"));
    }

    String name = (req.name() == null ? "" : req.name().trim());
    if (name.isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }

    Syllabus updated = new Syllabus(id, name, req.description(), current.getVisibility(), current.getOwnerId());
    Syllabus saved = contentService.createSyllabus(updated);
    boolean isEditable = admin || (saved.getVisibility() == Visibility.PRIVATE
        && caller.getId().equals(saved.getOwnerId()));
    return ResponseEntity.ok(new ManageSyllabusResponse(saved.getId(), saved.getName(),
        saved.getDescription(), saved.getVisibility() != null ? saved.getVisibility().name() : null, isEditable));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AppUser caller = resolveUser(principal);
    boolean admin = isAdmin(principal);
    try {
      contentService.deleteSyllabus(id, caller.getId(), admin);
      return ResponseEntity.ok().build();
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
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

  public record SyllabusRequest(String name, String description, String visibility) {}

  public record ManageSyllabusResponse(UUID id, String name, String description,
                                        String visibility, boolean isEditable) {}
}
