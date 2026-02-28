package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardEntity;
import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewLogEntity;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardJpaMapper;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewJpaMapper;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewLogJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardRepository;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewLogRepository;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewRepository;
import com.akdemya.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
    FlashcardJpaMapper.class,
    FlashcardReviewJpaMapper.class,
    FlashcardReviewLogJpaMapper.class
})
class FlashcardPersistenceAdapterTest {

  @Autowired JpaFlashcardRepository flashcardRepo;
  @Autowired JpaFlashcardReviewRepository reviewRepo;
  @Autowired JpaFlashcardReviewLogRepository logRepo;
  @Autowired FlashcardJpaMapper flashcardMapper;
  @Autowired FlashcardReviewJpaMapper reviewMapper;
  @Autowired FlashcardReviewLogJpaMapper logMapper;

  private FlashcardRepositoryAdapter flashcardAdapter;
  private FlashcardReviewRepositoryAdapter reviewAdapter;
  private FlashcardReviewLogRepositoryAdapter logAdapter;

  private final UUID unitId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    flashcardAdapter = new FlashcardRepositoryAdapter(flashcardRepo, flashcardMapper);
    reviewAdapter    = new FlashcardReviewRepositoryAdapter(reviewRepo, reviewMapper);
    logAdapter       = new FlashcardReviewLogRepositoryAdapter(logRepo, logMapper);
  }

  // ── Flashcard ─────────────────────────────────────────────────────────────

  @Test
  void saveAndFindFlashcardById() {
    Flashcard card = Flashcard.create(unitId, "Capital de Francia", "París");
    Flashcard saved = flashcardAdapter.save(card);

    var found = flashcardAdapter.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Capital de Francia", found.get().getFront());
    assertEquals("París", found.get().getBack());
  }

  @Test
  void findByUnitIdReturnsOnlyCardsForThatUnit() {
    UUID otherUnit = UUID.randomUUID();
    flashcardAdapter.save(Flashcard.create(unitId, "Q1", "A1"));
    flashcardAdapter.save(Flashcard.create(unitId, "Q2", "A2"));
    flashcardAdapter.save(Flashcard.create(otherUnit, "Q3", "A3"));

    List<Flashcard> cards = flashcardAdapter.findByUnitId(unitId);
    assertEquals(2, cards.size());
    assertTrue(cards.stream().allMatch(c -> c.getUnitId().equals(unitId)));
  }

  @Test
  void deleteFlashcardById() {
    Flashcard card = flashcardAdapter.save(Flashcard.create(unitId, "front", "back"));
    flashcardAdapter.deleteById(card.getId());
    assertTrue(flashcardAdapter.findById(card.getId()).isEmpty());
  }

  // ── FlashcardReview ───────────────────────────────────────────────────────

  @Test
  void saveAndFindReviewByUserIdAndFlashcardId() {
    UUID flashcardId = UUID.randomUUID();
    FlashcardReview review = FlashcardReview.createNew(userId, flashcardId, LocalDateTime.now());
    reviewAdapter.save(review);

    var found = reviewAdapter.findByUserIdAndFlashcardId(userId, flashcardId);
    assertTrue(found.isPresent());
    assertEquals(ReviewState.NEW, found.get().getState());
    assertEquals(2.5, found.get().getEaseFactor());
  }

  @Test
  void saveTwiceSameUserCardUpserts() {
    UUID flashcardId = UUID.randomUUID();
    LocalDateTime due = LocalDateTime.now();
    FlashcardReview review = FlashcardReview.createNew(userId, flashcardId, due);
    reviewAdapter.save(review);

    // apply a fake SM-2 update
    review.update(ReviewState.LEARNING, 2.3, 1, 0, 1, 0, due.plusDays(1));
    reviewAdapter.save(review);

    List<FlashcardReviewEntity> all = reviewRepo.findAll();
    assertEquals(1, all.size(), "Upsert must not create a duplicate row");
    assertEquals(ReviewState.LEARNING, all.get(0).getState());
  }

  @Test
  void findDueByUserIdReturnsCardsOrderedByDueAtAsc() {
    LocalDateTime now = LocalDateTime.now();
    UUID card1 = UUID.randomUUID();
    UUID card2 = UUID.randomUUID();
    UUID card3 = UUID.randomUUID();

    // card3 is due in the future — should NOT appear
    reviewAdapter.save(FlashcardReview.createNew(userId, card1, now.minusDays(2)));
    reviewAdapter.save(FlashcardReview.createNew(userId, card2, now.minusDays(1)));
    reviewAdapter.save(FlashcardReview.createNew(userId, card3, now.plusDays(1)));

    List<FlashcardReview> due = reviewAdapter.findDueByUserId(userId, now, 10);
    assertEquals(2, due.size());
    assertTrue(due.get(0).getDueAt().isBefore(due.get(1).getDueAt()));
  }

  @Test
  void findDueByUserIdRespectsLimit() {
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i < 5; i++) {
      reviewAdapter.save(FlashcardReview.createNew(userId, UUID.randomUUID(), now.minusDays(i + 1)));
    }
    List<FlashcardReview> due = reviewAdapter.findDueByUserId(userId, now, 3);
    assertEquals(3, due.size());
  }

  // ── FlashcardReviewLog ────────────────────────────────────────────────────

  @Test
  void saveLogAndFindRecentByUserId() {
    UUID flashcardId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    FlashcardReviewLog log1 = FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.GOOD, now.minusMinutes(5), 0, 1, 2.5, 2.5);
    FlashcardReviewLog log2 = FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.EASY, now, 1, 3, 2.5, 2.6);

    logAdapter.save(log1);
    logAdapter.save(log2);

    List<FlashcardReviewLog> recent = logAdapter.findRecentByUserId(userId, 10);
    assertEquals(2, recent.size());
    // Most recent first
    assertEquals(ReviewGrade.EASY, recent.get(0).getGrade());
    assertEquals(ReviewGrade.GOOD, recent.get(1).getGrade());
  }

  @Test
  void findRecentByUserIdRespectsLimit() {
    LocalDateTime now = LocalDateTime.now();
    UUID flashcardId = UUID.randomUUID();
    for (int i = 0; i < 4; i++) {
      logAdapter.save(FlashcardReviewLog.create(
          userId, flashcardId, ReviewGrade.AGAIN, now.minusMinutes(i), null, null, null, null));
    }
    assertEquals(2, logAdapter.findRecentByUserId(userId, 2).size());
  }
}
