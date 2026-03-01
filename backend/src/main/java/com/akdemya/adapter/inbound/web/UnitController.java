package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Unit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    private final ContentManagement contentService;

    public UnitController(ContentManagement contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<Unit> getBySubject(@RequestParam UUID subjectId) {
        return contentService.getUnitsBySubject(subjectId);
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Unit create(@RequestBody Unit u) {
        return contentService.createUnit(u);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        contentService.deleteUnit(id);
        return ResponseEntity.ok().build();
    }
}
