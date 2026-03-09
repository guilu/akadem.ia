package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.port.in.SourceDocumentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sources")
public class SourceDocumentController {

    private static final long MAX_SIZE = 50 * 1024 * 1024L; // 50 MB

    private final SourceDocumentUseCase useCase;

    public SourceDocumentController(SourceDocumentUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(error("file_required"));
        }
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(error("file_too_large"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains("pdf")) {
            return ResponseEntity.badRequest().body(error("only_pdf_supported"));
        }

        SourceDocumentUseCase.UploadCommand cmd = new SourceDocumentUseCase.UploadCommand(
                file.getOriginalFilename(), contentType, file.getBytes()
        );
        SourceDocumentUseCase.UploadResult result = useCase.upload(cmd);
        return ResponseEntity.ok(toResponse(result.document()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SourceDocumentResponse> list() {
        return useCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(toResponse(useCase.findById(id)));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private SourceDocumentResponse toResponse(SourceDocument d) {
        return new SourceDocumentResponse(d.getId(), d.getName(), d.getType(),
                d.getVersion(), d.getChecksum(), d.getUploadedAt(), d.getStatus().name());
    }

    private java.util.Map<String, String> error(String code) {
        return java.util.Map.of("error", code);
    }

    record SourceDocumentResponse(UUID id, String name, String type, String version,
                                   String checksum, LocalDateTime uploadedAt, String status) {}
}
