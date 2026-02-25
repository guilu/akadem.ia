package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
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

  public FlashcardStudyService(FlashcardRepository flashcardRepo, FlashcardReviewRepository reviewRepo) {
    this.flashcardRepo = flashcardRepo;
    this.reviewRepo = reviewRepo;
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
