package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaFlashcardRepository extends JpaRepository<FlashcardEntity, UUID> {

  List<FlashcardEntity> findByUnitId(UUID unitId);

  List<FlashcardEntity> findByIdIn(List<UUID> ids);

  @Query("""
      select f from FlashcardEntity f
      where f.unitId = :unitId
        and (f.visibility = 'GLOBAL' or f.ownerId = :userId)
        and not exists (
          select 1 from FlashcardReviewEntity r
          where r.userId = :userId and r.flashcardId = f.id
        )
      order by f.createdAt asc
      """)
  List<FlashcardEntity> findNewByUserIdAndUnitId(@Param("userId") UUID userId,
                                                 @Param("unitId") UUID unitId,
                                                 Pageable pageable);

  @Query("""
      select count(f) from FlashcardEntity f
      where f.unitId = :unitId
        and (f.visibility = 'GLOBAL' or f.ownerId = :userId)
        and not exists (
          select 1 from FlashcardReviewEntity r
          where r.userId = :userId and r.flashcardId = f.id
        )
      """)
  long countNewByUserIdAndUnitId(@Param("userId") UUID userId,
                                 @Param("unitId") UUID unitId);

  @Query("""
      select count(f) from FlashcardEntity f
      where (f.visibility = 'GLOBAL' or f.ownerId = :userId)
        and not exists (
          select 1 from FlashcardReviewEntity r
          where r.userId = :userId and r.flashcardId = f.id
        )
      """)
  long countNewByUserId(@Param("userId") UUID userId);

  @Query("SELECT f FROM FlashcardEntity f WHERE f.visibility = 'GLOBAL' OR f.ownerId = :userId")
  List<FlashcardEntity> findVisibleByUserId(@Param("userId") UUID userId);

  @Query("SELECT f FROM FlashcardEntity f WHERE f.unitId = :unitId AND (f.visibility = 'GLOBAL' OR f.ownerId = :userId)")
  List<FlashcardEntity> findVisibleByUnitIdAndUserId(@Param("unitId") UUID unitId, @Param("userId") UUID userId);

  @Query("""
      select f.unitId, count(f) from FlashcardEntity f
      where (f.visibility = 'GLOBAL' or f.ownerId = :userId)
        and not exists (
          select 1 from FlashcardReviewEntity r
          where r.userId = :userId and r.flashcardId = f.id
        )
      group by f.unitId
      """)
  List<Object[]> countNewByUserIdGroupByUnit(@Param("userId") UUID userId);
}
