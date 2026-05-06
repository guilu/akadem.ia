package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.UserRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final ContentManagement contentService;
    private final UserRepository userRepo;

    public SubjectController(ContentManagement contentService, UserRepository userRepo) {
        this.contentService = contentService;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Subject> getAll(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            // Unauthenticated — return only global subjects
            return contentService.getAllSubjects();
        }
        UUID userId = resolveUserId(principal);
        return contentService.getVisibleSubjects(userId);
    }

    @PostMapping
    public ResponseEntity<Subject> create(@Valid @RequestBody CreateSubjectRequest req,
                                          @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = resolveUserId(principal);
        Visibility visibility = req.visibility() != null ? req.visibility() : Visibility.PRIVATE;

        // Only ADMINs can create GLOBAL subjects
        if (visibility == Visibility.GLOBAL && !isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Subject subject;
        if (visibility == Visibility.GLOBAL) {
            subject = Subject.createGlobal(req.name(), req.description());
        } else {
            subject = Subject.createPrivate(req.name(), req.description(), userId);
        }
        return ResponseEntity.ok(contentService.createSubject(subject));
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
            contentService.deleteSubjectIfAuthorized(id, userId, admin);
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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

    public record CreateSubjectRequest(String name, String description, Visibility visibility) {}
}
