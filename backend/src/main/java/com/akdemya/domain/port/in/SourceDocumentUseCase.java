package com.akdemya.domain.port.in;

import com.akdemya.domain.model.SourceDocument;

import java.util.List;
import java.util.UUID;

public interface SourceDocumentUseCase {

    record UploadCommand(String filename, String contentType, byte[] bytes) {}

    record UploadResult(SourceDocument document) {}

    /** Upload and fully process a document (extract → chunk → embed → store). */
    UploadResult upload(UploadCommand command);

    List<SourceDocument> listAll();

    SourceDocument findById(UUID id);
}
