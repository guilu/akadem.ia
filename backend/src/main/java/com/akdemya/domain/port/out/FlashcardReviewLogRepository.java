package com.akdemya.domain.port.out;

import com.akdemya.domain.model.FlashcardReviewLog;

import java.util.List;
import java.util.UUID;

public interface FlashcardReviewLogRepository {

  FlashcardReviewLog save(FlashcardReviewLog log);

  List<FlashcardReviewLog> findRecentByUserId(UUID userId, int limit);
}
