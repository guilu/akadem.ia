package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.model.StudySettingsDefaults;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserSettingsRepository;
import com.akdemya.domain.service.Sm2Scheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FlashcardStudyService implements FlashcardStudyUseCase {

  private final FlashcardRepository flashcardRepo;
  private final FlashcardReviewRepository reviewRepo;
  private final UserSettingsRepository settingsRepo;
  private final UnitRepository unitRepo;
  private final SubjectRepository subjectRepo;
  private final Sm2Scheduler scheduler;

  public FlashcardStudyService(FlashcardRepository flashcardRepo,
                               FlashcardReviewRepository reviewRepo,
                               UserSettingsRepository settingsRepo,
                               UnitRepository unitRepo,
                               SubjectRepository subjectRepo,
                               FlashcardSchedulerProperties schedulerProperties) {
    this.flashcardRepo = flashcardRepo;
    this.reviewRepo = reviewRepo;
    this.settingsRepo = settingsRepo;
    this.unitRepo = unitRepo;
    this.subjectRepo = subjectRepo;
    this.scheduler = new Sm2Scheduler(
        schedulerProperties.getLearningStepsMinutes(),
        schedulerProperties.getEasyIntervalDays(),
        schedulerProperties.getReviewAgainRelearnMinutes()
    );
  }

  private int newLimit(UUID userId) {
    return settingsRepo.findByUserId(userId)
        .map(s -> s.newCardsLimit())
        .orElse(StudySettingsDefaults.DEFAULT_NEW_LIMIT);
  }

  @Override
  public StudyQueueResponse getStudyQueue(StudyQueueCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    LocalDateTime now = command.now() != null ? command.now() : LocalDateTime.now();

    long learningCount;
    long dueCount;
    long newCount;

    if (command.unitId() == null) {
      learningCount = reviewRepo.countDueByUserIdAndStateIn(command.userId(), now, List.of(ReviewState.LEARNING));
      dueCount = reviewRepo.countDueByUserIdAndStateIn(command.userId(), now, List.of(ReviewState.REVIEW));
      newCount = flashcardRepo.countNewByUserId(command.userId());
    } else {
      learningCount = reviewRepo.countDueByUserIdAndUnitIdAndStateIn(
          command.userId(), command.unitId(), now, List.of(ReviewState.LEARNING));
      dueCount = reviewRepo.countDueByUserIdAndUnitIdAndStateIn(
          command.userId(), command.unitId(), now, List.of(ReviewState.REVIEW));
      newCount = flashcardRepo.countNewByUserIdAndUnitId(command.userId(), command.unitId());
    }

    long cappedNew = Math.min(newCount, newLimit(command.userId()));
    return new StudyQueueResponse(cappedNew, dueCount, learningCount);
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
          ReviewState.NEW, review.getDueAt(),
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

  @Override
  public List<UnitSummaryResult> getUnitSummaries(UnitSummaryCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    LocalDateTime now = command.now() != null ? command.now() : LocalDateTime.now();

    Map<UUID, Long> newCounts = flashcardRepo.countNewByUserIdGroupByUnit(command.userId());
    Map<UUID, Long> reviewCounts = reviewRepo.countByUserIdAndStateInGroupByUnit(
        command.userId(), List.of(ReviewState.LEARNING, ReviewState.REVIEW));
    Map<UUID, Long> dueCounts = reviewRepo.countDueByUserIdUpToGroupByUnit(command.userId(), now);

    Map<UUID, String> subjectNames = subjectRepo.findAll().stream()
        .collect(Collectors.toMap(Subject::getId, Subject::getName));

    return unitRepo.findAllWithFlashcards().stream()
        .map(unit -> new UnitSummaryResult(
            unit.getId(),
            unit.getName(),
            unit.getSubjectId(),
            subjectNames.getOrDefault(unit.getSubjectId(), ""),
            newCounts.getOrDefault(unit.getId(), 0L),
            reviewCounts.getOrDefault(unit.getId(), 0L),
            dueCounts.getOrDefault(unit.getId(), 0L)
        ))
        .toList();
  }
}
