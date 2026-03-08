package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.SourceChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSourceChunkRepository extends JpaRepository<SourceChunkEntity, UUID> {
    List<SourceChunkEntity> findBySourceDocumentIdOrderByChunkIndex(UUID sourceDocumentId);
}
