package com.akdemya.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardJpaMapper;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewJpaMapper;
import com.akdemya.adapter.outbound.persistence.mapper.FlashcardReviewLogJpaMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardRepository;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewLogRepository;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewRepository;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.show_sql=false"
})
@Import({
    FlashcardJpaMapper.class,
    FlashcardReviewJpaMapper.class,
    FlashcardReviewLogJpaMapper.class
})
class FlashcardPersistenceAdapterTest {

  @Autowired
  JpaFlashcardRepository flashcardRepo;
  @Autowired
  JpaFlashcardReviewRepository reviewRepo;
  @Autowired
  JpaFlashcardReviewLogRepository logRepo;
  @Autowired
  FlashcardJpaMapper flashcardMapper;
  @Autowired
  FlashcardReviewJpaMapper reviewMapper;
  @Autowired
  FlashcardReviewLogJpaMapper logMapper;

  private FlashcardRepositoryAdapter flashcardAdapter;
  private FlashcardReviewRepositoryAdapter reviewAdapter;
  private FlashcardReviewLogRepositoryAdapter logAdapter;

  private final UUID unitId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    flashcardAdapter = new FlashcardRepositoryAdapter(flashcardRepo, flashcardMapper);
    reviewAdapter = new FlashcardReviewRepositoryAdapter(reviewRepo, reviewMapper);
    logAdapter = new FlashcardReviewLogRepositoryAdapter(logRepo, logMapper);
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

  @Test
  void findByUserIdAndFlashcardIdAndReviewedAt_returnsMatchingLog() {
    LocalDateTime now = LocalDateTime.now().withNano(0); // truncate for DB precision
    UUID flashcardId = UUID.randomUUID();
    logAdapter.save(FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.GOOD, now, null, null, 2.5, 2.5));

    var found = logAdapter.findByUserIdAndFlashcardIdAndReviewedAt(userId, flashcardId, now);
    assertTrue(found.isPresent());
    assertEquals(ReviewGrade.GOOD, found.get().getGrade());
  }

  @Test
  void findByUserIdAndFlashcardIdAndReviewedAt_returnsEmptyWhenNotFound() {
    var found = logAdapter.findByUserIdAndFlashcardIdAndReviewedAt(
        userId, UUID.randomUUID(), LocalDateTime.now());
    assertTrue(found.isEmpty());
  }

  @Test
  void findMostRecentByUserIdAndFlashcardIdAfter_returnsLatestAfterCutoff() {
    LocalDateTime now = LocalDateTime.now().withNano(0);
    UUID flashcardId = UUID.randomUUID();
    LocalDateTime cutoff = now.minusDays(2);

    logAdapter.save(FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.AGAIN, now.minusDays(3), null, null, 2.5, 2.5)); // before cutoff
    logAdapter.save(FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.GOOD, now.minusDays(1), null, null, 2.5, 2.5)); // after cutoff
    logAdapter.save(FlashcardReviewLog.create(
        userId, flashcardId, ReviewGrade.EASY, now, null, null, 2.5, 2.6)); // most recent after cutoff

    var found = logAdapter.findMostRecentByUserIdAndFlashcardIdAfter(userId, flashcardId, cutoff);
    assertTrue(found.isPresent());
    assertEquals(ReviewGrade.EASY, found.get().getGrade());
  }

  // ── FlashcardRepositoryAdapter additional methods ─────────────────────────

  @Test
  void findByIdsReturnsMatchingCards() {
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "Q1", "A1"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "Q2", "A2"));
    flashcardAdapter.save(Flashcard.create(unitId, "Q3", "A3"));

    List<Flashcard> result = flashcardAdapter.findByIds(List.of(c1.getId(), c2.getId()));
    assertEquals(2, result.size());
  }

  @Test
  void findByIdsWithEmptyListReturnsEmpty() {
    List<Flashcard> result = flashcardAdapter.findByIds(List.of());
    assertTrue(result.isEmpty());
  }

  @Test
  void findNewByUserIdAndUnitId_returnsCardsWithNoReview() {
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "New", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "Reviewed", "A"));

    // c2 has a review
    reviewAdapter.save(FlashcardReview.createNew(userId, c2.getId(), LocalDateTime.now()));

    List<Flashcard> newCards = flashcardAdapter.findNewByUserIdAndUnitId(userId, unitId, 10);
    assertEquals(1, newCards.size());
    assertEquals(c1.getId(), newCards.get(0).getId());
  }

  @Test
  void countNewByUserIdAndUnitId_countsCorrectly() {
    flashcardAdapter.save(Flashcard.create(unitId, "New1", "A"));
    Flashcard reviewed = flashcardAdapter.save(Flashcard.create(unitId, "Reviewed", "A"));
    reviewAdapter.save(FlashcardReview.createNew(userId, reviewed.getId(), LocalDateTime.now()));

    long count = flashcardAdapter.countNewByUserIdAndUnitId(userId, unitId);
    assertEquals(1, count);
  }

  @Test
  void countNewByUserId_countsAcrossAllUnits() {
    UUID unitId2 = UUID.randomUUID();
    flashcardAdapter.save(Flashcard.create(unitId, "New1", "A"));
    flashcardAdapter.save(Flashcard.create(unitId2, "New2", "A"));
    Flashcard reviewed = flashcardAdapter.save(Flashcard.create(unitId, "Reviewed", "A"));
    reviewAdapter.save(FlashcardReview.createNew(userId, reviewed.getId(), LocalDateTime.now()));

    long count = flashcardAdapter.countNewByUserId(userId);
    assertEquals(2, count);
  }

  @Test
  void findVisibleByUserId_returnsGlobalAndOwnedCards() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();

    Flashcard global = flashcardAdapter.save(Flashcard.createGlobal(unitId, "Global", "A"));
    Flashcard mine = flashcardAdapter.save(Flashcard.createPrivate(unitId, "Mine", "A", ownerId));
    flashcardAdapter.save(Flashcard.createPrivate(unitId, "Other", "A", otherId));

    List<Flashcard> visible = flashcardAdapter.findVisibleByUserId(ownerId);
    List<UUID> ids = visible.stream().map(Flashcard::getId).toList();
    assertTrue(ids.contains(global.getId()));
    assertTrue(ids.contains(mine.getId()));
  }

  @Test
  void findVisibleByUnitIdAndUserId_filtersCorrectly() {
    UUID ownerId = UUID.randomUUID();
    UUID otherUnit = UUID.randomUUID();

    Flashcard inUnit = flashcardAdapter.save(Flashcard.createGlobal(unitId, "InUnit", "A"));
    flashcardAdapter.save(Flashcard.createGlobal(otherUnit, "OtherUnit", "A"));

    List<Flashcard> result = flashcardAdapter.findVisibleByUnitIdAndUserId(unitId, ownerId);
    assertEquals(1, result.size());
    assertEquals(inUnit.getId(), result.get(0).getId());
  }

  @Test
  void countNewByUserIdGroupByUnit_delegatesToJpaQuery() {
    UUID unit1 = UUID.randomUUID();
    UUID unit2 = UUID.randomUUID();
    flashcardAdapter.save(Flashcard.create(unit1, "New1", "A"));
    flashcardAdapter.save(Flashcard.create(unit1, "New2", "A"));
    flashcardAdapter.save(Flashcard.create(unit2, "New3", "A"));
    Flashcard reviewed = flashcardAdapter.save(Flashcard.create(unit1, "Reviewed", "A"));
    reviewAdapter.save(FlashcardReview.createNew(userId, reviewed.getId(), LocalDateTime.now()));

    Map<UUID, Long> result = flashcardAdapter.countNewByUserIdGroupByUnit(userId);

    assertEquals(2L, result.getOrDefault(unit1, 0L));
    assertEquals(1L, result.getOrDefault(unit2, 0L));
  }

  // ── FlashcardReviewRepositoryAdapter additional methods ───────────────────

  @Test
  void findDueByUserIdAndUnitId_returnsOnlyDueCardsInUnit() {
    LocalDateTime now = LocalDateTime.now();
    UUID unit2 = UUID.randomUUID();

    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "due-in-unit", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unit2, "due-other-unit", "A"));
    Flashcard c3 = flashcardAdapter.save(Flashcard.create(unitId, "future-in-unit", "A"));

    reviewAdapter.save(FlashcardReview.createNew(userId, c1.getId(), now.minusHours(1)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c2.getId(), now.minusHours(1)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c3.getId(), now.plusHours(1)));

    List<FlashcardReview> due = reviewAdapter.findDueByUserIdAndUnitId(userId, unitId, now, 10);
    assertEquals(1, due.size());
    assertEquals(c1.getId(), due.get(0).getFlashcardId());
  }

  @Test
  void countDueByUserIdAndUnitIdUpTo_countsCorrectly() {
    LocalDateTime now = LocalDateTime.now();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "due", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "future", "A"));
    reviewAdapter.save(FlashcardReview.createNew(userId, c1.getId(), now.minusHours(1)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c2.getId(), now.plusHours(1)));

    long count = reviewAdapter.countDueByUserIdAndUnitIdUpTo(userId, unitId, now);
    assertEquals(1, count);
  }

  @Test
  void countDueByUserIdAndUnitIdAndStateIn_filtersState() {
    LocalDateTime now = LocalDateTime.now();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "learning", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "review", "A"));

    FlashcardReview r1 = FlashcardReview.createNew(userId, c1.getId(), now.minusHours(1));
    r1.update(ReviewState.LEARNING, 2.5, 1, 1, 1, 0, now.minusMinutes(10));
    reviewAdapter.save(r1);

    FlashcardReview r2 = FlashcardReview.createNew(userId, c2.getId(), now.minusDays(1));
    r2.update(ReviewState.REVIEW, 2.5, 1, 0, 2, 0, now.minusDays(1));
    reviewAdapter.save(r2);

    long learningCount = reviewAdapter.countDueByUserIdAndUnitIdAndStateIn(
        userId, unitId, now, List.of(ReviewState.LEARNING));
    assertEquals(1, learningCount);
  }

  @Test
  void countByUserIdAndUnitIdAndStateIn_countsRegardlessOfDue() {
    LocalDateTime now = LocalDateTime.now();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "future-learning", "A"));
    FlashcardReview r1 = FlashcardReview.createNew(userId, c1.getId(), now.plusDays(5));
    r1.update(ReviewState.LEARNING, 2.5, 1, 1, 1, 0, now.plusDays(5));
    reviewAdapter.save(r1);

    long count = reviewAdapter.countByUserIdAndUnitIdAndStateIn(
        userId, unitId, List.of(ReviewState.LEARNING));
    assertEquals(1, count);
  }

  @Test
  void countDueByUserIdAndUnitIdBetween_countsInRange() {
    LocalDateTime now = LocalDateTime.now();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "in-range", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "out-range", "A"));
    reviewAdapter.save(FlashcardReview.createNew(userId, c1.getId(), now.plusDays(2)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c2.getId(), now.plusDays(10)));

    long count = reviewAdapter.countDueByUserIdAndUnitIdBetween(
        userId, unitId, now, now.plusDays(3));
    assertEquals(1, count);
  }

  @Test
  void countDueByUserIdAndStateIn_countsGloballyAcrossUnits() {
    LocalDateTime now = LocalDateTime.now();
    UUID unit2 = UUID.randomUUID();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "due1", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unit2, "due2", "A"));
    Flashcard c3 = flashcardAdapter.save(Flashcard.create(unitId, "future", "A"));

    FlashcardReview r1 = FlashcardReview.createNew(userId, c1.getId(), now.minusHours(1));
    r1.update(ReviewState.REVIEW, 2.5, 1, 0, 2, 0, now.minusHours(1));
    reviewAdapter.save(r1);

    FlashcardReview r2 = FlashcardReview.createNew(userId, c2.getId(), now.minusHours(2));
    r2.update(ReviewState.REVIEW, 2.5, 1, 0, 2, 0, now.minusHours(2));
    reviewAdapter.save(r2);

    reviewAdapter.save(FlashcardReview.createNew(userId, c3.getId(), now.plusDays(1)));

    long count = reviewAdapter.countDueByUserIdAndStateIn(
        userId, now, List.of(ReviewState.REVIEW));
    assertEquals(2, count);
  }

  @Test
  void countByUserIdAndStateInGroupByUnit_returnsGroupedCounts() {
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "learning1", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unitId, "learning2", "A"));
    UUID unit2 = UUID.randomUUID();
    Flashcard c3 = flashcardAdapter.save(Flashcard.create(unit2, "review1", "A"));

    FlashcardReview r1 = FlashcardReview.createNew(userId, c1.getId(), LocalDateTime.now().plusDays(1));
    r1.update(ReviewState.LEARNING, 2.5, 1, 1, 1, 0, LocalDateTime.now().plusDays(1));
    reviewAdapter.save(r1);

    FlashcardReview r2 = FlashcardReview.createNew(userId, c2.getId(), LocalDateTime.now().plusDays(2));
    r2.update(ReviewState.LEARNING, 2.5, 1, 1, 1, 0, LocalDateTime.now().plusDays(2));
    reviewAdapter.save(r2);

    FlashcardReview r3 = FlashcardReview.createNew(userId, c3.getId(), LocalDateTime.now().plusDays(3));
    r3.update(ReviewState.REVIEW, 2.5, 7, 0, 3, 0, LocalDateTime.now().plusDays(3));
    reviewAdapter.save(r3);

    Map<UUID, Long> result = reviewAdapter.countByUserIdAndStateInGroupByUnit(
        userId, List.of(ReviewState.LEARNING, ReviewState.REVIEW));

    assertEquals(2L, result.getOrDefault(unitId, 0L));
    assertEquals(1L, result.getOrDefault(unit2, 0L));
  }

  @Test
  void countDueByUserIdUpToGroupByUnit_returnsGroupedDueCounts() {
    LocalDateTime now = LocalDateTime.now();
    UUID unit2 = UUID.randomUUID();
    Flashcard c1 = flashcardAdapter.save(Flashcard.create(unitId, "due1", "A"));
    Flashcard c2 = flashcardAdapter.save(Flashcard.create(unit2, "due2", "A"));
    Flashcard c3 = flashcardAdapter.save(Flashcard.create(unitId, "future", "A"));

    reviewAdapter.save(FlashcardReview.createNew(userId, c1.getId(), now.minusHours(1)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c2.getId(), now.minusHours(2)));
    reviewAdapter.save(FlashcardReview.createNew(userId, c3.getId(), now.plusDays(1)));

    Map<UUID, Long> result = reviewAdapter.countDueByUserIdUpToGroupByUnit(userId, now);

    assertEquals(1L, result.getOrDefault(unitId, 0L));
    assertEquals(1L, result.getOrDefault(unit2, 0L));
  }
}
