package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.SourceChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataSourceChunkRepository extends JpaRepository<SourceChunkEntity, UUID> {
    List<SourceChunkEntity> findBySourceDocumentIdOrderByChunkIndex(UUID sourceDocumentId);
    List<SourceChunkEntity> findByUnitIdOrderByChunkIndex(UUID unitId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE SourceChunkEntity c SET c.unitId = :unitId WHERE c.id = :chunkId")
    void updateUnitId(@Param("chunkId") UUID chunkId, @Param("unitId") UUID unitId);
}
