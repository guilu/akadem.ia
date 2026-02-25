package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewLogJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewLogRepository;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FlashcardReviewLogRepositoryAdapter implements FlashcardReviewLogRepository {

  private final JpaFlashcardReviewLogRepository jpa;
  private final FlashcardReviewLogJpaMapper mapper;

  public FlashcardReviewLogRepositoryAdapter(JpaFlashcardReviewLogRepository jpa,
                                             FlashcardReviewLogJpaMapper mapper) {
    this.jpa = jpa;
    this.mapper = mapper;
  }

  @Override
  public FlashcardReviewLog save(FlashcardReviewLog log) {
    return mapper.toDomain(jpa.save(mapper.toEntity(log)));
  }

  @Override
  public Optional<FlashcardReviewLog> findByUserIdAndFlashcardIdAndReviewedAt(UUID userId, UUID flashcardId,
                                                                              LocalDateTime reviewedAt) {
    return jpa.findByUserIdAndFlashcardIdAndReviewedAt(
            userId, flashcardId, reviewedAt.toInstant(ZoneOffset.UTC))
        .map(mapper::toDomain);
  }

  @Override
  public List<FlashcardReviewLog> findRecentByUserId(UUID userId, int limit) {
    return jpa.findByUserIdOrderByReviewedAtDesc(userId, PageRequest.of(0, limit))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
