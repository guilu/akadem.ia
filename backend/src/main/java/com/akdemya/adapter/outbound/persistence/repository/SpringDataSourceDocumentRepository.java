package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.SourceDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSourceDocumentRepository extends JpaRepository<SourceDocumentEntity, UUID> {
    List<SourceDocumentEntity> findBySubjectId(UUID subjectId);
}
