package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceDocumentRepository {
    SourceDocument save(SourceDocument document);
    Optional<SourceDocument> findById(UUID id);
    List<SourceDocument> findAll();
    List<SourceDocument> findBySubjectId(UUID subjectId);
}
