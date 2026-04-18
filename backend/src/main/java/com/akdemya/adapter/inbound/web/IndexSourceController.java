package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.IndexSourceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sources")
public class IndexSourceController {

    private static final long MAX_SIZE = 50 * 1024 * 1024L; // 50 MB

    private final IndexSourceUseCase useCase;

    public IndexSourceController(IndexSourceUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Step 1: Upload PDF.
     * Returns the saved document (PENDING_REVIEW) + list of detected units for user review.
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subjectId") UUID subjectId) throws Exception {

        if (file.isEmpty()) return ResponseEntity.badRequest().body(error("file_required"));
        if (file.getSize() > MAX_SIZE) return ResponseEntity.badRequest().body(error("file_too_large"));
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains("pdf")) {
            return ResponseEntity.badRequest().body(error("only_pdf_supported"));
        }

        IndexSourceUseCase.UploadCommand cmd = new IndexSourceUseCase.UploadCommand(
                subjectId, file.getOriginalFilename(), contentType, file.getBytes()
        );
        IndexSourceUseCase.UploadPreview result;
        try {
            result = useCase.upload(cmd);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("document_already_exists:")) {
                String docName = e.getMessage().substring("document_already_exists:".length());
                return ResponseEntity.status(409).body(Map.of(
                        "error", "document_already_exists",
                        "message", "El documento \"" + docName + "\" ya ha sido procesado. Elimínalo primero para volver a subirlo."
                ));
            }
            throw e;
        }

        return ResponseEntity.ok(new UploadPreviewResponse(
                toDocResponse(result.document()),
                result.detectedUnits().stream()
                        .map(u -> new DetectedUnitResponse(u.name(), u.headingKey(), u.chunkCount()))
                        .toList()
        ));
    }

    /**
     * Step 2: Confirm index — user approves detected units (optionally renamed).
     * Creates Unit records, assigns unit_id to chunks, marks document PROCESSED.
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmRequest req) {

        if (req.approvedUnits() == null || req.approvedUnits().isEmpty()) {
            return ResponseEntity.badRequest().body(error("approved_units_required"));
        }

        List<IndexSourceUseCase.ApprovedUnit> approved = req.approvedUnits().stream()
                .map(u -> new IndexSourceUseCase.ApprovedUnit(u.headingKey(), u.name(), u.description()))
                .toList();

        try {
            IndexSourceUseCase.ConfirmResult result = useCase.confirm(
                    new IndexSourceUseCase.ConfirmCommand(id, approved)
            );
            return ResponseEntity.ok(new ConfirmResponse(
                    toDocResponse(result.document()),
                    result.savedUnits().stream().map(this::toUnitResponse).toList()
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SourceDocumentResponse> list(@RequestParam(required = false) UUID subjectId) {
        List<SourceDocument> docs = subjectId != null
                ? useCase.listBySubject(subjectId)
                : useCase.listAll();
        return docs.stream().map(this::toDocResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(toDocResponse(useCase.findById(id)));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            useCase.deleteSource(id);
            return ResponseEntity.noContent().build();
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private SourceDocumentResponse toDocResponse(SourceDocument d) {
        return new SourceDocumentResponse(d.getId(), d.getSubjectId(), d.getName(), d.getType(),
                d.getVersion(), d.getChecksum(), d.getUploadedAt(), d.getStatus().name());
    }

    private UnitResponse toUnitResponse(Unit u) {
        return new UnitResponse(u.getId(), u.getSubjectId(), u.getName(), u.getDescription(), u.getOrderIndex());
    }

    private Map<String, String> error(String code) {
        return Map.of("error", code);
    }

    record SourceDocumentResponse(UUID id, UUID subjectId, String name, String type, String version,
                                   String checksum, LocalDateTime uploadedAt, String status) {}

    record DetectedUnitResponse(String name, String headingKey, int chunkCount) {}

    record UploadPreviewResponse(SourceDocumentResponse document, List<DetectedUnitResponse> detectedUnits) {}

    record ApprovedUnitRequest(String headingKey, String name, String description) {}

    record ConfirmRequest(List<ApprovedUnitRequest> approvedUnits) {}

    record UnitResponse(UUID id, UUID subjectId, String name, String description, int orderIndex) {}

    record ConfirmResponse(SourceDocumentResponse document, List<UnitResponse> savedUnits) {}
}
