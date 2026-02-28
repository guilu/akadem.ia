package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewRepository;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FlashcardReviewRepositoryAdapter implements FlashcardReviewRepository {

  private final JpaFlashcardReviewRepository jpa;
  private final FlashcardReviewJpaMapper mapper;

  public FlashcardReviewRepositoryAdapter(JpaFlashcardReviewRepository jpa, FlashcardReviewJpaMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId) {
    return jpa.findByUserIdAndFlashcardId(userId, flashcardId).map(mapper::toDomain);
  }

  @Override
  public List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit) {
    return jpa.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(
            userId,
            upTo.toInstant(ZoneOffset.UTC),
            PageRequest.of(0, limit))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<FlashcardReview> findDueByUserIdAndUnitId(UUID userId, UUID unitId, LocalDateTime upTo, int limit) {
    return jpa.findDueByUserIdAndUnitId(
            userId,
            unitId,
            upTo.toInstant(ZoneOffset.UTC),
            PageRequest.of(0, limit))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countDueByUserIdAndUnitIdUpTo(UUID userId, UUID unitId, LocalDateTime upTo) {
    return jpa.countDueByUserIdAndUnitIdUpTo(userId, unitId, upTo.toInstant(ZoneOffset.UTC));
  }

  @Override
  public long countByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, List<ReviewState> states) {
    return jpa.countByUserIdAndUnitIdAndStateIn(userId, unitId, states);
  }

  @Override
  public long countDueByUserIdAndUnitIdBetween(UUID userId, UUID unitId,
                                               LocalDateTime fromExclusive, LocalDateTime toInclusive) {
    return jpa.countDueByUserIdAndUnitIdBetween(
        userId, unitId, fromExclusive.toInstant(ZoneOffset.UTC), toInclusive.toInstant(ZoneOffset.UTC));
  }

  /**
   * Upsert: if a review already exists for (userId, flashcardId) patch its SRS fields;
   * otherwise insert a new row. This avoids unique-constraint violations on re-saves.
   */
  @Override
  public FlashcardReview save(FlashcardReview review) {
    Optional<FlashcardReviewEntity> existing =
        jpa.findByUserIdAndFlashcardId(review.getUserId(), review.getFlashcardId());

    FlashcardReviewEntity entity;
    if (existing.isPresent()) {
      entity = existing.get();
      mapper.updateEntity(entity, review);
    } else {
      entity = mapper.toEntity(review);
    }
    return mapper.toDomain(jpa.save(entity));
  }
}
