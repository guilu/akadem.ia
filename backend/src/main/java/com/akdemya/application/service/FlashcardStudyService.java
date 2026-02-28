package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.service.Sm2Scheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FlashcardStudyService implements FlashcardStudyUseCase {

  private static final int NEW_LIMIT = 10;

  private final FlashcardRepository flashcardRepo;
  private final FlashcardReviewRepository reviewRepo;
  private final Sm2Scheduler scheduler;

  public FlashcardStudyService(FlashcardRepository flashcardRepo,
                               FlashcardReviewRepository reviewRepo,
                               FlashcardSchedulerProperties schedulerProperties) {
    this.flashcardRepo = flashcardRepo;
    this.reviewRepo = reviewRepo;
    this.scheduler = new Sm2Scheduler(
        schedulerProperties.getLearningStepsMinutes(),
        schedulerProperties.getEasyIntervalDays(),
        schedulerProperties.getReviewAgainRelearnMinutes()
    );
  }

  @Override
  public StudyQueueResponse getStudyQueue(StudyQueueCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    if (command.unitId() == null) throw new IllegalArgumentException("unitId cannot be null");
    int limit = Math.max(0, command.limit());
    LocalDateTime now = command.now() != null ? command.now() : LocalDateTime.now();

    List<FlashcardReview> due = reviewRepo.findDueByUserIdAndUnitId(command.userId(), command.unitId(), now, limit);
    List<UUID> dueIds = due.stream().map(FlashcardReview::getFlashcardId).toList();
    Map<UUID, Flashcard> dueFlashcards = flashcardRepo.findByIds(dueIds).stream()
        .collect(Collectors.toMap(Flashcard::getId, f -> f));

    List<StudyQueueItem> items = new ArrayList<>();
    for (FlashcardReview review : due) {
      Flashcard card = dueFlashcards.get(review.getFlashcardId());
      if (card == null) continue;
      items.add(new StudyQueueItem(
          card.getId(),
          card.getFront(),
          card.getBack(),
          review.getState(),
          review.getDueAt()
      ));
    }

    int remaining = limit - items.size();
    if (remaining > 0) {
      int newLimit = Math.min(remaining, NEW_LIMIT);
      List<Flashcard> newCards = flashcardRepo.findNewByUserIdAndUnitId(command.userId(), command.unitId(), newLimit);
      for (Flashcard card : newCards) {
        items.add(new StudyQueueItem(card.getId(), card.getFront(), card.getBack(), null, null));
      }
    }

    return new StudyQueueResponse(items);
  }

  @Override
  public StudyNextResponse getStudyNext(StudyNextCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    if (command.unitId() == null) throw new IllegalArgumentException("unitId cannot be null");
    LocalDateTime now = command.now() != null ? command.now() : LocalDateTime.now();

    List<FlashcardReview> due = reviewRepo.findDueByUserIdAndUnitId(command.userId(), command.unitId(), now, 1);
    if (!due.isEmpty()) {
      FlashcardReview review = due.get(0);
      Flashcard card = flashcardRepo.findById(review.getFlashcardId()).orElse(null);
      if (card != null) {
        return new StudyNextResponse(
            card.getId(), card.getUnitId(), card.getFront(), card.getBack(),
            review.getState(), review.getDueAt(),
            toIntervalHints(scheduler.intervalHints(review, now))
        );
      }
    }

    List<Flashcard> newCards = flashcardRepo.findNewByUserIdAndUnitId(command.userId(), command.unitId(), 1);
    if (!newCards.isEmpty()) {
      Flashcard card = newCards.get(0);
      FlashcardReview review = FlashcardReview.createNew(command.userId(), card.getId(), now);
      return new StudyNextResponse(
          card.getId(), card.getUnitId(), card.getFront(), card.getBack(),
          ReviewState.NEW, null,
          toIntervalHints(scheduler.intervalHints(review, now))
      );
    }

    return null;
  }

  private IntervalHints toIntervalHints(Sm2Scheduler.IntervalHints hints) {
    return new IntervalHints(hints.again(), hints.good(), hints.easy());
  }

  @Override
  public DashboardResponse getDashboard(DashboardCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    if (command.unitId() == null) throw new IllegalArgumentException("unitId cannot be null");
    LocalDateTime now = command.now() != null ? command.now() : LocalDateTime.now();

    long dueToday = reviewRepo.countDueByUserIdAndUnitIdUpTo(command.userId(), command.unitId(), now);
    long dueIn1to3 = reviewRepo.countDueByUserIdAndUnitIdBetween(command.userId(), command.unitId(), now, now.plusDays(3));
    long dueIn4to7 = reviewRepo.countDueByUserIdAndUnitIdBetween(command.userId(), command.unitId(), now.plusDays(3), now.plusDays(7));
    long dueIn8to30 = reviewRepo.countDueByUserIdAndUnitIdBetween(command.userId(), command.unitId(), now.plusDays(7), now.plusDays(30));
    long newCards = flashcardRepo.countNewByUserIdAndUnitId(command.userId(), command.unitId());

    long totalDue = dueToday + dueIn1to3 + dueIn4to7 + dueIn8to30;

    return new DashboardResponse(dueToday, dueIn1to3, dueIn4to7, dueIn8to30, newCards, totalDue);
  }
}
