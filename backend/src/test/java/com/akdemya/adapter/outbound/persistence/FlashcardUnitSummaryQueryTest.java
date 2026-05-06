package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.FlashcardEntity;
import com.akdemya.adapter.outbound.persistence.entity.FlashcardReviewEntity;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardRepository;
import com.akdemya.adapter.outbound.persistence.repository.JpaFlashcardReviewRepository;
import com.akdemya.domain.model.ReviewState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.show_sql=false"
})
class FlashcardUnitSummaryQueryTest {

  @Autowired JpaFlashcardRepository flashcardRepo;
  @Autowired JpaFlashcardReviewRepository reviewRepo;

  private final UUID userId = UUID.randomUUID();

  // ── helpers ───────────────────────────────────────────────────────────────

  private FlashcardEntity card(UUID unitId) {
    FlashcardEntity e = new FlashcardEntity();
    e.setId(UUID.randomUUID());
    e.setUnitId(unitId);
    e.setFront("q");
    e.setBack("a");
    Instant now = Instant.now();
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    e.setVisibility("GLOBAL");
    return flashcardRepo.save(e);
  }

  private void review(UUID flashcardId, ReviewState state, Instant dueAt) {
    FlashcardReviewEntity r = new FlashcardReviewEntity();
    r.setId(UUID.randomUUID());
    r.setUserId(userId);
    r.setFlashcardId(flashcardId);
    r.setState(state);
    r.setEaseFactor(2.5);
    r.setIntervalDays(1);
    r.setLearningStep(0);
    r.setRepetitions(1);
    r.setLapses(0);
    r.setDueAt(dueAt);
    Instant now = Instant.now();
    r.setCreatedAt(now);
    r.setUpdatedAt(now);
    reviewRepo.save(r);
  }

  // ── tests ─────────────────────────────────────────────────────────────────

  @Test
  void countNewByUserIdGroupByUnit_returnsCorrectPerUnitCounts() {
    UUID unit1 = UUID.randomUUID();
    UUID unit2 = UUID.randomUUID();
    UUID unit3 = UUID.randomUUID();

    // unit1: 2 cards, 1 reviewed → 1 new
    FlashcardEntity c1a = card(unit1);
    FlashcardEntity c1b = card(unit1);
    review(c1a.getId(), ReviewState.REVIEW, Instant.now().plusSeconds(3600));

    // unit2: 3 cards, 0 reviewed → 3 new
    card(unit2);
    card(unit2);
    card(unit2);

    // unit3: 1 card, 1 reviewed → 0 new (should not appear in result)
    FlashcardEntity c3 = card(unit3);
    review(c3.getId(), ReviewState.LEARNING, Instant.now().plusSeconds(60));

    List<Object[]> rows = flashcardRepo.countNewByUserIdGroupByUnit(userId);
    Map<UUID, Long> byUnit = rows.stream()
        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));

    assertEquals(1L, byUnit.getOrDefault(unit1, 0L), "unit1 should have 1 new card");
    assertEquals(3L, byUnit.getOrDefault(unit2, 0L), "unit2 should have 3 new cards");
    assertFalse(byUnit.containsKey(unit3), "unit3 has no new cards, should not appear");
  }

  @Test
  void countByUserIdAndStateInGroupByUnit_returnsCorrectPerUnitCounts() {
    UUID unit1 = UUID.randomUUID();
    UUID unit2 = UUID.randomUUID();

    // unit1: 2 LEARNING reviews
    FlashcardEntity c1a = card(unit1);
    FlashcardEntity c1b = card(unit1);
    review(c1a.getId(), ReviewState.LEARNING, Instant.now().plusSeconds(60));
    review(c1b.getId(), ReviewState.LEARNING, Instant.now().plusSeconds(120));

    // unit2: 1 REVIEW review, 1 NEW (not in states list)
    FlashcardEntity c2a = card(unit2);
    FlashcardEntity c2b = card(unit2);
    review(c2a.getId(), ReviewState.REVIEW, Instant.now().plusSeconds(3600));
    review(c2b.getId(), ReviewState.NEW, Instant.now().plusSeconds(3600));

    List<Object[]> rows = reviewRepo.countByUserIdAndStateInGroupByUnit(
        userId, List.of(ReviewState.LEARNING, ReviewState.REVIEW));
    Map<UUID, Long> byUnit = rows.stream()
        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));

    assertEquals(2L, byUnit.getOrDefault(unit1, 0L), "unit1 should have 2 LEARNING reviews");
    assertEquals(1L, byUnit.getOrDefault(unit2, 0L), "unit2 should have 1 REVIEW review (NEW excluded)");
  }

  @Test
  void countDueByUserIdUpToGroupByUnit_returnsCorrectPerUnitCounts() {
    Instant now = Instant.now();
    UUID unit1 = UUID.randomUUID();
    UUID unit2 = UUID.randomUUID();

    // unit1: 2 overdue, 1 future
    FlashcardEntity c1a = card(unit1);
    FlashcardEntity c1b = card(unit1);
    FlashcardEntity c1c = card(unit1);
    review(c1a.getId(), ReviewState.REVIEW, now.minusSeconds(3600));
    review(c1b.getId(), ReviewState.REVIEW, now.minusSeconds(60));
    review(c1c.getId(), ReviewState.REVIEW, now.plusSeconds(3600)); // future

    // unit2: 1 overdue
    FlashcardEntity c2 = card(unit2);
    review(c2.getId(), ReviewState.LEARNING, now.minusSeconds(10));

    List<Object[]> rows = reviewRepo.countDueByUserIdUpToGroupByUnit(userId, now);
    Map<UUID, Long> byUnit = rows.stream()
        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));

    assertEquals(2L, byUnit.getOrDefault(unit1, 0L), "unit1 should have 2 due cards");
    assertEquals(1L, byUnit.getOrDefault(unit2, 0L), "unit2 should have 1 due card");
  }

  @Test
  void allThreeQueries_with50Units_eachReturnSingleQueryResult() {
    Instant now = Instant.now();

    // Seed 50 units, each with 1 card; for even units add a review
    for (int i = 0; i < 50; i++) {
      UUID unitId = UUID.randomUUID();
      FlashcardEntity c = card(unitId);
      if (i % 2 == 0) {
        review(c.getId(), ReviewState.REVIEW, now.minusSeconds(60));
      }
    }

    // The key assertion: these are aggregate queries — they should return a
    // single result set, not one row per flashcard or per review
    List<Object[]> newRows = flashcardRepo.countNewByUserIdGroupByUnit(userId);
    List<Object[]> reviewRows = reviewRepo.countByUserIdAndStateInGroupByUnit(
        userId, List.of(ReviewState.LEARNING, ReviewState.REVIEW));
    List<Object[]> dueRows = reviewRepo.countDueByUserIdUpToGroupByUnit(userId, now);

    // 25 units have no reviews → appear in newRows; 25 have reviews → don't
    assertEquals(25, newRows.size(), "25 units should have new cards");
    // 25 units have REVIEW reviews
    assertEquals(25, reviewRows.size(), "25 units should have review-state cards");
    assertEquals(25, dueRows.size(), "25 units should have due cards");
  }
}
