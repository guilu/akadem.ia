package com.akdemya.domain.port.in;

import com.akdemya.domain.model.ReviewState;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FlashcardStudyUseCase {

  StudyQueueResponse getStudyQueue(StudyQueueCommand command);

  StudyNextResponse getStudyNext(StudyNextCommand command);

  DashboardResponse getDashboard(DashboardCommand command);

  record StudyQueueCommand(UUID userId, UUID unitId, int limit, LocalDateTime now) {}

  record StudyQueueResponse(long newCount, long dueCount, long learningCount) {}

  record StudyNextCommand(UUID userId, UUID unitId, LocalDateTime now) {}

  record IntervalHints(String again, String good, String easy) {}

  record StudyNextResponse(UUID flashcardId, UUID unitId, String front, String back,
                           ReviewState state, LocalDateTime dueAt,
                           IntervalHints intervalHints) {}

  record DashboardCommand(UUID userId, UUID unitId, LocalDateTime now) {}

  record DashboardResponse(long dueToday,
                           long dueIn1to3Days,
                           long dueIn4to7Days,
                           long dueIn8to30Days,
                           long newCards,
                           long totalDue) {}
}
