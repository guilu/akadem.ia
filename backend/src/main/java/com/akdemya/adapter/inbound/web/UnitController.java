package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Unit;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final ContentManagement contentService;
    private final UserRepository userRepo;

    public UnitController(ContentManagement contentService, UserRepository userRepo) {
        this.contentService = contentService;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Unit> getBySubject(@RequestParam UUID subjectId,
                                   @AuthenticationPrincipal User principal) {
        if (principal == null) {
            // Unauthenticated — return only global units
            return contentService.getUnitsBySubject(subjectId);
        }
        UUID userId = resolveUserId(principal);
        return contentService.getVisibleUnitsBySubject(subjectId, userId);
    }

    @GetMapping("/availability")
    public List<ContentManagement.UnitAvailability> getAvailability(@RequestParam UUID subjectId,
            @RequestParam(required = false) String difficulty) {
        com.akdemya.domain.model.Question.Difficulty diff = null;
        if (difficulty != null && !difficulty.isBlank()) {
            try {
                diff = com.akdemya.domain.model.Question.Difficulty.valueOf(difficulty);
            } catch (IllegalArgumentException ignored) {
            }
        }
        org.slf4j.LoggerFactory.getLogger(UnitController.class)
            .info("Availability for subject {} difficulty {}", subjectId, difficulty);
        return contentService.getUnitAvailability(subjectId, diff);
    }

    @PostMapping
    public ResponseEntity<Unit> create(@RequestBody CreateUnitRequest req,
                                       @AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = resolveUserId(principal);
        Visibility visibility = req.visibility() != null ? req.visibility() : Visibility.PRIVATE;

        // Only ADMINs can create GLOBAL units
        if (visibility == Visibility.GLOBAL && !isAdmin(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Unit unit;
        if (visibility == Visibility.GLOBAL) {
            unit = Unit.createGlobal(req.subjectId(), req.name(), req.description(), req.orderIndex());
        } else {
            unit = Unit.createPrivate(req.subjectId(), req.name(), req.description(), req.orderIndex(), userId);
        }
        return ResponseEntity.ok(contentService.createUnit(unit));
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
            contentService.deleteUnitIfAuthorized(id, userId, admin);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean isAdmin(User principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public record CreateUnitRequest(UUID subjectId, String name, String description,
                                    int orderIndex, Visibility visibility) {}
}
