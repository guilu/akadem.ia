package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardStudyServiceTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID unitId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.of(2026, 2, 24, 10, 0);

  @Test
  void dueCardsReturnedBeforeNew() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard dueCard = flashcard("Front due", unitId);
    Flashcard newCard = flashcard("Front new", unitId);
    flashcardRepo.save(dueCard);
    flashcardRepo.save(newCard);

    reviewRepo.save(review(dueCard.getId(), now.minusDays(1)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 2, now));

    assertEquals(2, response.items().size());
    assertEquals(dueCard.getId(), response.items().get(0).flashcardId());
    assertEquals(newCard.getId(), response.items().get(1).flashcardId());
  }

  @Test
  void newCardsLimitedToTen() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    for (int i = 0; i < 15; i++) {
      flashcardRepo.save(flashcard("Card " + i, unitId));
    }

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 20, now));

    assertEquals(10, response.items().size());
  }

  @Test
  void noDuplicatesBetweenDueAndNew() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard dueCard = flashcard("Due", unitId);
    Flashcard other = flashcard("New", unitId);
    flashcardRepo.save(dueCard);
    flashcardRepo.save(other);

    reviewRepo.save(review(dueCard.getId(), now.minusDays(2)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 5, now));

    Set<UUID> ids = response.items().stream().map(FlashcardStudyUseCase.StudyQueueItem::flashcardId)
        .collect(Collectors.toSet());
    assertEquals(response.items().size(), ids.size());
    assertTrue(ids.contains(dueCard.getId()));
    assertTrue(ids.contains(other.getId()));
  }

  @Test
  void dueCardsOrderedByDueAtAsc() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard c1 = flashcard("c1", unitId);
    Flashcard c2 = flashcard("c2", unitId);
    flashcardRepo.save(c1);
    flashcardRepo.save(c2);

    reviewRepo.save(review(c2.getId(), now.minusDays(1)));
    reviewRepo.save(review(c1.getId(), now.minusDays(3)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 5, now));

    assertEquals(c1.getId(), response.items().get(0).flashcardId());
    assertEquals(c2.getId(), response.items().get(1).flashcardId());
  }

  @Test
  void dashboardCountsAreCoherent() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard today = flashcard("today", unitId);
    Flashcard in2 = flashcard("in2", unitId);
    Flashcard in5 = flashcard("in5", unitId);
    Flashcard in10 = flashcard("in10", unitId);
    Flashcard newCard = flashcard("new", unitId);
    flashcardRepo.save(today);
    flashcardRepo.save(in2);
    flashcardRepo.save(in5);
    flashcardRepo.save(in10);
    flashcardRepo.save(newCard);

    reviewRepo.save(review(today.getId(), now.minusHours(1)));
    reviewRepo.save(review(in2.getId(), now.plusDays(2)));
    reviewRepo.save(review(in5.getId(), now.plusDays(5)));
    reviewRepo.save(review(in10.getId(), now.plusDays(10)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo);
    var dashboard = service.getDashboard(new FlashcardStudyUseCase.DashboardCommand(userId, unitId, now));

    assertEquals(1, dashboard.dueToday());
    assertEquals(1, dashboard.dueIn1to3Days());
    assertEquals(1, dashboard.dueIn4to7Days());
    assertEquals(1, dashboard.dueIn8to30Days());
    assertEquals(1, dashboard.newCards());
    assertEquals(4, dashboard.totalDue());
  }

  private Flashcard flashcard(String front, UUID unitId) {
    return new Flashcard(UUID.randomUUID(), unitId, front, "back", now.minusDays(10), now.minusDays(1));
  }

  private FlashcardReview review(UUID flashcardId, LocalDateTime dueAt) {
    return new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 1, 1, 0, dueAt, now.minusDays(1), now.minusDays(10), now.minusDays(1));
  }

  static class InMemoryFlashcardRepo implements FlashcardRepository {
    private final Map<UUID, Flashcard> data = new ConcurrentHashMap<>();
    private InMemoryReviewRepo reviewRepo;

    void attach(InMemoryReviewRepo reviewRepo) {
      this.reviewRepo = reviewRepo;
    }

    @Override
    public Optional<Flashcard> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Flashcard> findByIds(List<UUID> ids) {
      if (ids == null || ids.isEmpty()) return List.of();
      return ids.stream().map(data::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<Flashcard> findByUnitId(UUID unitId) {
      return data.values().stream().filter(f -> f.getUnitId().equals(unitId)).toList();
    }

    @Override
    public List<Flashcard> findNewByUserIdAndUnitId(UUID userId, UUID unitId, int limit) {
      List<Flashcard> cards = findByUnitId(unitId).stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .sorted(Comparator.comparing(Flashcard::getCreatedAt))
          .toList();
      return cards.subList(0, Math.min(limit, cards.size()));
    }

    @Override
    public long countNewByUserIdAndUnitId(UUID userId, UUID unitId) {
      return findByUnitId(unitId).stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .count();
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
  }

  static class InMemoryReviewRepo implements FlashcardReviewRepository {
    private final Map<UUID, FlashcardReview> data = new ConcurrentHashMap<>();
    private final InMemoryFlashcardRepo flashcardRepo;

    InMemoryReviewRepo(InMemoryFlashcardRepo flashcardRepo) {
      this.flashcardRepo = flashcardRepo;
    }

    @Override
    public Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && r.getFlashcardId().equals(flashcardId))
          .findFirst();
    }

    @Override
    public List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && !r.getDueAt().isAfter(upTo))
          .sorted(Comparator.comparing(FlashcardReview::getDueAt))
          .limit(limit)
          .toList();
    }

    @Override
    public List<FlashcardReview> findDueByUserIdAndUnitId(UUID userId, UUID unitId, LocalDateTime upTo, int limit) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && !r.getDueAt().isAfter(upTo))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .sorted(Comparator.comparing(FlashcardReview::getDueAt))
          .limit(limit)
          .toList();
    }

    @Override
    public long countDueByUserIdAndUnitIdUpTo(UUID userId, UUID unitId, LocalDateTime upTo) {
      return findDueByUserIdAndUnitId(userId, unitId, upTo, Integer.MAX_VALUE).size();
    }

    @Override
    public long countDueByUserIdAndUnitIdBetween(UUID userId, UUID unitId, LocalDateTime fromExclusive,
                                                 LocalDateTime toInclusive) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId))
          .filter(r -> r.getDueAt().isAfter(fromExclusive) && !r.getDueAt().isAfter(toInclusive))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .count();
    }

    @Override
    public FlashcardReview save(FlashcardReview review) {
      data.put(review.getId(), review);
      return review;
    }
  }
}
