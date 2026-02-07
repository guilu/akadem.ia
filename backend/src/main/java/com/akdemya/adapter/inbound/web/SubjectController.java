package com.akdemya.adapter.inbound.web;

import com.akdemya.application.service.ContentManagement;
import com.akdemya.domain.model.Subject;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    private final ContentManagement contentService;

    public SubjectController(ContentManagement contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<Subject> getAll() {
        return contentService.getAllSubjects();
    }

    @PostMapping
    public Subject create(@RequestBody Subject s) {
        return contentService.createSubject(s);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        contentService.deleteSubject(id);
        return ResponseEntity.ok().build();
    }
}
