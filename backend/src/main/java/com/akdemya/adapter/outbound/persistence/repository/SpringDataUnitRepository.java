package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SpringDataUnitRepository extends JpaRepository<UnitEntity, UUID> {
    List<UnitEntity> findBySubject_IdOrderByOrderIndexAscNameAsc(UUID subjectId);

    @Query("SELECT u FROM UnitEntity u WHERE u.subject.id = :subjectId AND (u.visibility = 'GLOBAL' OR u.ownerId = :userId) ORDER BY u.orderIndex ASC, u.name ASC")
    List<UnitEntity> findVisibleBySubjectIdAndUserId(@Param("subjectId") UUID subjectId, @Param("userId") UUID userId);

    @Query("SELECT u FROM UnitEntity u WHERE u.subject.id = :subjectId AND u.visibility = 'GLOBAL' ORDER BY u.orderIndex ASC, u.name ASC")
    List<UnitEntity> findGlobalBySubjectId(@Param("subjectId") UUID subjectId);

    @Query("SELECT u FROM UnitEntity u WHERE u.subject.id = :subjectId AND u.visibility = 'PRIVATE' AND u.ownerId = :userId ORDER BY u.orderIndex ASC, u.name ASC")
    List<UnitEntity> findPrivateBySubjectIdAndOwner(@Param("subjectId") UUID subjectId, @Param("userId") UUID userId);

    @Query("SELECT u FROM UnitEntity u WHERE u.subject.id = :subjectId AND (u.visibility = 'GLOBAL' OR (u.visibility = 'PRIVATE' AND u.ownerId = :userId)) ORDER BY u.orderIndex ASC, u.name ASC")
    List<UnitEntity> findAllBySubjectIdAndScope(@Param("subjectId") UUID subjectId, @Param("userId") UUID userId);

    @Query("""
        select u from UnitEntity u
        where u.subject.id = :subjectId
          and exists (select 1 from FlashcardEntity f where f.unitId = u.id)
        order by u.orderIndex asc, u.name asc
        """)
    List<UnitEntity> findBySubjectIdWithFlashcards(UUID subjectId);

    @Query("""
        select u from UnitEntity u
        where exists (select 1 from FlashcardEntity f where f.unitId = u.id)
        order by u.orderIndex asc, u.name asc
        """)
    List<UnitEntity> findAllWithFlashcards();

    @Query("""
        select u from UnitEntity u
        where (u.visibility = 'GLOBAL' OR u.ownerId = :userId)
          and exists (select 1 from FlashcardEntity f where f.unitId = u.id)
        order by u.orderIndex asc, u.name asc
        """)
    List<UnitEntity> findVisibleWithFlashcardsByUserId(@Param("userId") UUID userId);
}
