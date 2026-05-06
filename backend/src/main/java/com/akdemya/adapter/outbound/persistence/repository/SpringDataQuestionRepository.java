package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    List<QuestionEntity> findByUnit_Id(UUID unitId);
    org.springframework.data.domain.Page<QuestionEntity> findByUnit_Id(UUID unitId, org.springframework.data.domain.Pageable pageable);
    long countByUnit_Id(UUID unitId);
    long countByUnit_IdAndDifficulty(UUID unitId, String difficulty);

    @Query("SELECT q FROM QuestionEntity q WHERE q.visibility = 'GLOBAL' OR q.ownerId = :userId")
    List<QuestionEntity> findVisibleByUserId(@Param("userId") UUID userId);

    @Query("SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId AND (q.visibility = 'GLOBAL' OR q.ownerId = :userId)")
    List<QuestionEntity> findVisibleByUnitIdAndUserId(@Param("unitId") UUID unitId, @Param("userId") UUID userId);

    @Query("SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId AND q.visibility = 'GLOBAL'")
    org.springframework.data.domain.Page<QuestionEntity> findGlobalByUnitId(@Param("unitId") UUID unitId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId AND q.visibility = 'PRIVATE' AND q.ownerId = :userId")
    org.springframework.data.domain.Page<QuestionEntity> findPrivateByUnitIdAndOwner(@Param("unitId") UUID unitId, @Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId AND (q.visibility = 'GLOBAL' OR (q.visibility = 'PRIVATE' AND q.ownerId = :userId))")
    org.springframework.data.domain.Page<QuestionEntity> findAllByUnitIdAndScope(@Param("unitId") UUID unitId, @Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);
}
