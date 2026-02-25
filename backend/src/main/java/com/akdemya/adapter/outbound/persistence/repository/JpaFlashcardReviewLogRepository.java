package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFlashcardReviewLogRepository extends JpaRepository<FlashcardReviewLogEntity, UUID> {

  List<FlashcardReviewLogEntity> findByUserIdOrderByReviewedAtDesc(UUID userId, Pageable pageable);

  Optional<FlashcardReviewLogEntity> findByUserIdAndFlashcardIdAndReviewedAt(UUID userId, UUID flashcardId,
                                                                           Instant reviewedAt);
}
