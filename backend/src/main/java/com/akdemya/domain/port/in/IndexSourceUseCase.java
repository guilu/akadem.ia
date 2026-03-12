package com.akdemya.domain.port.in;

import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;

import java.util.List;
import java.util.UUID;

public interface IndexSourceUseCase {

    record UploadCommand(UUID subjectId, String filename, String contentType, byte[] bytes) {}

    record DetectedUnit(String name, String headingKey, int chunkCount) {}

    record UploadPreview(SourceDocument document, List<DetectedUnit> detectedUnits) {}

    record ApprovedUnit(String headingKey, String name, String description) {}

    record ConfirmCommand(UUID documentId, List<ApprovedUnit> approvedUnits) {}

    record ConfirmResult(SourceDocument document, List<Unit> savedUnits) {}

    /**
     * Step 1: Upload PDF, extract text, chunk, detect unit headings.
     * Saves document as PENDING_REVIEW with chunks (no embeddings yet).
     * Returns detected units for user review.
     */
    UploadPreview upload(UploadCommand command);

    /**
     * Step 2: User approves/renames unit list. Creates Unit records in DB,
     * assigns unit_id to matching chunks, marks document as PROCESSED.
     */
    ConfirmResult confirm(ConfirmCommand command);

    List<SourceDocument> listAll();

    List<SourceDocument> listBySubject(UUID subjectId);

    SourceDocument findById(UUID id);

    /**
     * Delete source document and all associated data:
     * units derived from it (and their questions), plus chunks and drafts (cascade via DB).
     */
    void deleteSource(UUID id);
}
