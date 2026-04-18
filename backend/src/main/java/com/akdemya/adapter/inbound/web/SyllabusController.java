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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syllabuses")
public class SyllabusController {

  private final ContentManagement contentService;
  private final UserRepository userRepo;

  public SyllabusController(ContentManagement contentService, UserRepository userRepo) {
    this.contentService = contentService;
    this.userRepo = userRepo;
  }

  @GetMapping
  public List<SyllabusResponse> getAll(@AuthenticationPrincipal User principal) {
    UUID userId = principal != null ? resolveUserId(principal) : null;
    return contentService.getVisibleSyllabuses(userId).stream()
        .map(SyllabusResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateSyllabusRequest req,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (req.name() == null || req.name().trim().isEmpty()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "name_required"));
    }
    UUID userId = resolveUserId(principal);
    boolean admin = isAdmin(principal);

    Visibility visibility = req.visibility() != null ? req.visibility() : Visibility.PRIVATE;
    if (visibility == Visibility.GLOBAL && !admin) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(java.util.Map.of("error", "only_admins_can_create_global_syllabuses"));
    }

    Syllabus syllabus;
    if (visibility == Visibility.GLOBAL) {
      syllabus = Syllabus.createGlobal(req.name().trim(), req.description());
    } else {
      syllabus = Syllabus.createPrivate(req.name().trim(), req.description(), userId);
    }
    return ResponseEntity.ok(SyllabusResponse.from(contentService.createSyllabus(syllabus)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@PathVariable UUID id,
                                   @AuthenticationPrincipal User principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID userId = resolveUserId(principal);
    boolean admin = isAdmin(principal);
    try {
      contentService.deleteSyllabus(id, userId, admin);
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

  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable UUID id,
                                    @AuthenticationPrincipal User principal) {
    return contentService.getSyllabusById(id)
        .map(s -> ResponseEntity.ok(SyllabusResponse.from(s)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/{id}/subjects")
  public ResponseEntity<?> getSubjects(@PathVariable UUID id,
                                        @AuthenticationPrincipal User principal) {
    if (contentService.getSyllabusById(id).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    UUID userId = principal != null ? resolveUserId(principal) : null;
    List<Subject> subjects = contentService.getSubjectsBySyllabus(id, userId);
    return ResponseEntity.ok(subjects);
  }

  private UUID resolveUserId(User principal) {
    return userRepo.findByEmail(principal.getUsername())
        .map(AppUser::getId)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private boolean isAdmin(User principal) {
    return principal.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  public record CreateSyllabusRequest(String name, String description, Visibility visibility) {}

  public record SyllabusResponse(UUID id, String name, String description, String visibility, UUID ownerId) {
    static SyllabusResponse from(Syllabus s) {
      return new SyllabusResponse(s.getId(), s.getName(), s.getDescription(),
          s.getVisibility() != null ? s.getVisibility().name() : null, s.getOwnerId());
    }
  }
}
