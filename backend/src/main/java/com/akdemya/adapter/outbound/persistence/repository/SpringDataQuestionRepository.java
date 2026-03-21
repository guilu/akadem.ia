package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    List<QuestionEntity> findByUnit_Id(UUID unitId);
    org.springframework.data.domain.Page<QuestionEntity> findByUnit_Id(UUID unitId, org.springframework.data.domain.Pageable pageable);
    long countByUnit_Id(UUID unitId);
    long countByUnit_IdAndDifficulty(UUID unitId, String difficulty);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId ORDER BY CASE q.difficulty WHEN 'EASY' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'HARD' THEN 3 END ASC",
        countQuery = "SELECT count(q) FROM QuestionEntity q WHERE q.unit.id = :unitId"
    )
    org.springframework.data.domain.Page<QuestionEntity> findByUnit_IdSortByDifficultyAsc(
        @org.springframework.data.repository.query.Param("unitId") UUID unitId,
        org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT q FROM QuestionEntity q WHERE q.unit.id = :unitId ORDER BY CASE q.difficulty WHEN 'EASY' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'HARD' THEN 1 END ASC",
        countQuery = "SELECT count(q) FROM QuestionEntity q WHERE q.unit.id = :unitId"
    )
    org.springframework.data.domain.Page<QuestionEntity> findByUnit_IdSortByDifficultyDesc(
        @org.springframework.data.repository.query.Param("unitId") UUID unitId,
        org.springframework.data.domain.Pageable pageable);
}
