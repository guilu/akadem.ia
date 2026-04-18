package com.akdemya.domain.port.in;

import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;

import java.util.List;
import java.util.UUID;

public interface IndexSourceUseCase {

    record UploadCommand(UUID subjectId, String filename, String contentType, byte[] bytes) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UploadCommand that = (UploadCommand) o;
            return java.util.Objects.equals(subjectId, that.subjectId) &&
                   java.util.Objects.equals(filename, that.filename) &&
                   java.util.Objects.equals(contentType, that.contentType) &&
                   java.util.Arrays.equals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            int result = java.util.Objects.hash(subjectId, filename, contentType);
            result = 31 * result + java.util.Arrays.hashCode(bytes);
            return result;
        }

        @Override
        public String toString() {
            return "UploadCommand[" +
                   "subjectId=" + subjectId + ", " +
                   "filename=" + filename + ", " +
                   "contentType=" + contentType + ", " +
                   "bytes=" + java.util.Arrays.toString(bytes) + "]";
        }
    }

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
