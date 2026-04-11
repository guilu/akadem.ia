package com.akdemya.domain.port.out;

import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardReviewRepository {

  Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId);

  /** Returns up to {@code limit} cards due for {@code userId} at or before {@code upTo}, ordered by dueAt asc. */
  List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit);

  /** Returns up to {@code limit} cards due for {@code userId} and {@code unitId} at or before {@code upTo}, ordered by dueAt asc. */
  List<FlashcardReview> findDueByUserIdAndUnitId(UUID userId, UUID unitId, LocalDateTime upTo, int limit);

  long countDueByUserIdAndUnitIdUpTo(UUID userId, UUID unitId, LocalDateTime upTo);

  long countDueByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, LocalDateTime upTo,
                                           List<ReviewState> states);

  long countByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, List<ReviewState> states);

  long countDueByUserIdAndUnitIdBetween(UUID userId, UUID unitId, LocalDateTime fromExclusive, LocalDateTime toInclusive);

  long countDueByUserIdAndStateIn(UUID userId, LocalDateTime upTo, List<ReviewState> states);

  FlashcardReview save(FlashcardReview review);

  /** Returns count of reviews per unitId where state is in {@code states}, for the given user. */
  Map<UUID, Long> countByUserIdAndStateInGroupByUnit(UUID userId, List<ReviewState> states);

  /** Returns count of due reviews per unitId (dueAt &lt;= upTo), for the given user. */
  Map<UUID, Long> countDueByUserIdUpToGroupByUnit(UUID userId, LocalDateTime upTo);
}
