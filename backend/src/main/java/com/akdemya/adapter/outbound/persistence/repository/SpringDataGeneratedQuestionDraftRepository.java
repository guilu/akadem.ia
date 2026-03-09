package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.GeneratedQuestionDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataGeneratedQuestionDraftRepository extends JpaRepository<GeneratedQuestionDraftEntity, UUID> {
    List<GeneratedQuestionDraftEntity> findBySourceDocumentIdOrderByCreatedAtDesc(UUID sourceDocumentId);
    List<GeneratedQuestionDraftEntity> findBySourceDocumentIdAndStatusOrderByCreatedAtDesc(UUID sourceDocumentId, String status);
}
