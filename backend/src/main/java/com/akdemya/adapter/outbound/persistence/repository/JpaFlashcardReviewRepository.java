package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.domain.model.ReviewState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFlashcardReviewRepository extends JpaRepository<FlashcardReviewEntity, UUID> {

  Optional<FlashcardReviewEntity> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

  Page<FlashcardReviewEntity> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(
      UUID userId, Instant dueAt, Pageable pageable);

  @Query("""
      select r from FlashcardReviewEntity r
      join FlashcardEntity f on f.id = r.flashcardId
      where r.userId = :userId
        and f.unitId = :unitId
        and r.dueAt <= :dueAt
      order by r.dueAt asc
      """)
  List<FlashcardReviewEntity> findDueByUserIdAndUnitId(@Param("userId") UUID userId,
                                                      @Param("unitId") UUID unitId,
                                                      @Param("dueAt") Instant dueAt,
                                                      Pageable pageable);

  @Query("""
      select count(r) from FlashcardReviewEntity r
      join FlashcardEntity f on f.id = r.flashcardId
      where r.userId = :userId
        and f.unitId = :unitId
        and r.dueAt <= :upTo
      """)
  long countDueByUserIdAndUnitIdUpTo(@Param("userId") UUID userId,
                                     @Param("unitId") UUID unitId,
                                     @Param("upTo") Instant upTo);

  @Query("""
      select count(r) from FlashcardReviewEntity r
      join FlashcardEntity f on f.id = r.flashcardId
      where r.userId = :userId
        and f.unitId = :unitId
        and r.dueAt <= :upTo
        and r.state in :states
      """)
  long countDueByUserIdAndUnitIdAndStateIn(@Param("userId") UUID userId,
                                          @Param("unitId") UUID unitId,
                                          @Param("upTo") Instant upTo,
                                          @Param("states") List<ReviewState> states);

  @Query("""
      select count(r) from FlashcardReviewEntity r
      join FlashcardEntity f on f.id = r.flashcardId
      where r.userId = :userId
        and f.unitId = :unitId
        and r.state in :states
      """)
  long countByUserIdAndUnitIdAndStateIn(@Param("userId") UUID userId,
                                       @Param("unitId") UUID unitId,
                                       @Param("states") List<ReviewState> states);

  @Query("""
      select count(r) from FlashcardReviewEntity r
      join FlashcardEntity f on f.id = r.flashcardId
      where r.userId = :userId
        and f.unitId = :unitId
        and r.dueAt > :fromExclusive
        and r.dueAt <= :toInclusive
      """)
  long countDueByUserIdAndUnitIdBetween(@Param("userId") UUID userId,
                                       @Param("unitId") UUID unitId,
                                       @Param("fromExclusive") Instant fromExclusive,
                                       @Param("toInclusive") Instant toInclusive);
}
