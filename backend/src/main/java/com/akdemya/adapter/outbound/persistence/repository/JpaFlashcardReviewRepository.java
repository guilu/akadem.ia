package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JpaFlashcardReviewRepository extends JpaRepository<FlashcardReviewEntity, UUID> {

  Optional<FlashcardReviewEntity> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

  Page<FlashcardReviewEntity> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(
      UUID userId, Instant dueAt, Pageable pageable);
}
