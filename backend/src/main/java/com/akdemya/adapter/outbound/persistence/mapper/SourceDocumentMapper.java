package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SourceDocumentEntity;
import com.akdemya.domain.model.SourceDocument;
import org.springframework.stereotype.Component;

@Component
public class SourceDocumentMapper {

    public SourceDocument toDomain(SourceDocumentEntity e) {
        return new SourceDocument(
                e.getId(),
                e.getName(),
                e.getType(),
                e.getVersion(),
                e.getChecksum(),
                e.getUploadedAt(),
                SourceDocument.Status.valueOf(e.getStatus()),
                e.getStoragePath()
        );
    }

    public SourceDocumentEntity toEntity(SourceDocument d) {
        SourceDocumentEntity e = new SourceDocumentEntity();
        e.setId(d.getId());
        e.setName(d.getName());
        e.setType(d.getType());
        e.setVersion(d.getVersion());
        e.setChecksum(d.getChecksum());
        e.setUploadedAt(d.getUploadedAt());
        e.setStatus(d.getStatus().name());
        e.setStoragePath(d.getStoragePath());
        return e;
    }
}
