package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.*;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardReviewServiceTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID unitId = UUID.randomUUID();
  private final UUID flashcardId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.of(2026, 2, 25, 10, 0);

  @Test
  void upsertCreatesReviewAndLog() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo();
    InMemoryLogRepo logRepo = new InMemoryLogRepo();

    flashcardRepo.save(new Flashcard(flashcardId, unitId, "front", "back", now.minusDays(1), now.minusDays(1)));

    FlashcardReviewService service = new FlashcardReviewService(flashcardRepo, reviewRepo, logRepo, new FlashcardSchedulerProperties());
    var resp = service.registerReview(new FlashcardReviewUseCase.RegisterCommand(userId, flashcardId, ReviewGrade.GOOD, now));

    assertNotNull(resp.review());
    assertNotNull(resp.log());
    assertEquals(1, reviewRepo.data.size());
    assertEquals(1, logRepo.data.size());
  }

  @Test
  void idempotentOnSameReviewedAt() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo();
    InMemoryLogRepo logRepo = new InMemoryLogRepo();

    flashcardRepo.save(new Flashcard(flashcardId, unitId, "front", "back", now.minusDays(1), now.minusDays(1)));

    FlashcardReviewService service = new FlashcardReviewService(flashcardRepo, reviewRepo, logRepo, new FlashcardSchedulerProperties());
    service.registerReview(new FlashcardReviewUseCase.RegisterCommand(userId, flashcardId, ReviewGrade.GOOD, now));
    var resp2 = service.registerReview(new FlashcardReviewUseCase.RegisterCommand(userId, flashcardId, ReviewGrade.GOOD, now));

    assertEquals(1, logRepo.data.size());
    assertEquals(1, reviewRepo.data.size());
    assertNotNull(resp2.log());
  }

  @Test
  void transactionConsistencyWhenLogFails() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo();
    FailingLogRepo logRepo = new FailingLogRepo();

    flashcardRepo.save(new Flashcard(flashcardId, unitId, "front", "back", now.minusDays(1), now.minusDays(1)));

    FlashcardReviewService service = new FlashcardReviewService(flashcardRepo, reviewRepo, logRepo, new FlashcardSchedulerProperties());

    assertThrows(RuntimeException.class, () -> service.registerReview(
        new FlashcardReviewUseCase.RegisterCommand(userId, flashcardId, ReviewGrade.GOOD, now)));
  }

  static class InMemoryFlashcardRepo implements FlashcardRepository {
    private final Map<UUID, Flashcard> data = new ConcurrentHashMap<>();

    @Override
    public Optional<Flashcard> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Flashcard> findByIds(List<UUID> ids) {
      return ids.stream().map(data::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<Flashcard> findByUnitId(UUID unitId) {
      return data.values().stream().filter(f -> f.getUnitId().equals(unitId)).toList();
    }

    @Override
    public List<Flashcard> findNewByUserIdAndUnitId(UUID userId, UUID unitId, int limit) {
      return List.of();
    }

    @Override
    public long countNewByUserIdAndUnitId(UUID userId, UUID unitId) {
      return 0;
    }

    @Override
    public long countNewByUserId(UUID userId) {
      return 0;
    }

    @Override
    public Flashcard save(Flashcard flashcard) {
      data.put(flashcard.getId(), flashcard);
      return flashcard;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public java.util.List<Flashcard> findVisibleByUserId(UUID userId) {
      return data.values().stream()
          .filter(f -> f.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
              || userId.equals(f.getOwnerId()))
          .toList();
    }
  }

  static class InMemoryReviewRepo implements FlashcardReviewRepository {
    private final Map<UUID, FlashcardReview> data = new ConcurrentHashMap<>();

    @Override
    public Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && r.getFlashcardId().equals(flashcardId))
          .findFirst();
    }

    @Override
    public List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit) {
      return List.of();
    }

    @Override
    public List<FlashcardReview> findDueByUserIdAndUnitId(UUID userId, UUID unitId, LocalDateTime upTo, int limit) {
      return List.of();
    }

    @Override
    public long countDueByUserIdAndUnitIdUpTo(UUID userId, UUID unitId, LocalDateTime upTo) {
      return 0;
    }

    @Override
    public long countDueByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, LocalDateTime upTo,
                                                    List<ReviewState> states) {
      return 0;
    }

    @Override
    public long countByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, List<ReviewState> states) {
      return 0;
    }

    @Override
    public long countDueByUserIdAndUnitIdBetween(UUID userId, UUID unitId, LocalDateTime fromExclusive,
                                                 LocalDateTime toInclusive) {
      return 0;
    }

    @Override
    public long countDueByUserIdAndStateIn(UUID userId, LocalDateTime upTo, List<ReviewState> states) {
      return 0;
    }

    @Override
    public FlashcardReview save(FlashcardReview review) {
      data.put(review.getId(), review);
      return review;
    }
  }

  static class InMemoryLogRepo implements FlashcardReviewLogRepository {
    private final Map<UUID, FlashcardReviewLog> data = new ConcurrentHashMap<>();

    @Override
    public FlashcardReviewLog save(FlashcardReviewLog log) {
      data.put(log.getId(), log);
      return log;
    }

    @Override
    public Optional<FlashcardReviewLog> findByUserIdAndFlashcardIdAndReviewedAt(UUID userId, UUID flashcardId,
                                                                                LocalDateTime reviewedAt) {
      return data.values().stream()
          .filter(l -> l.getUserId().equals(userId))
          .filter(l -> l.getFlashcardId().equals(flashcardId))
          .filter(l -> l.getReviewedAt().equals(reviewedAt))
          .findFirst();
    }

    @Override
    public List<FlashcardReviewLog> findRecentByUserId(UUID userId, int limit) {
      return List.of();
    }

    @Override
    public Optional<FlashcardReviewLog> findMostRecentByUserIdAndFlashcardIdAfter(UUID userId, UUID flashcardId,
                                                                                  LocalDateTime after) {
      return data.values().stream()
          .filter(l -> l.getUserId().equals(userId))
          .filter(l -> l.getFlashcardId().equals(flashcardId))
          .filter(l -> l.getReviewedAt().isAfter(after))
          .max(Comparator.comparing(FlashcardReviewLog::getReviewedAt));
    }
  }

  static class FailingLogRepo implements FlashcardReviewLogRepository {
    @Override
    public FlashcardReviewLog save(FlashcardReviewLog log) {
      throw new RuntimeException("fail");
    }

    @Override
    public Optional<FlashcardReviewLog> findByUserIdAndFlashcardIdAndReviewedAt(UUID userId, UUID flashcardId,
                                                                                LocalDateTime reviewedAt) {
      return Optional.empty();
    }

    @Override
    public List<FlashcardReviewLog> findRecentByUserId(UUID userId, int limit) {
      return List.of();
    }

    @Override
    public Optional<FlashcardReviewLog> findMostRecentByUserIdAndFlashcardIdAfter(UUID userId, UUID flashcardId,
                                                                                  LocalDateTime after) {
      return Optional.empty();
    }
  }
}
