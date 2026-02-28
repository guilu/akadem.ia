package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.service.Sm2Scheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class FlashcardReviewService implements FlashcardReviewUseCase {

  private final FlashcardRepository flashcardRepo;
  private final FlashcardReviewRepository reviewRepo;
  private final FlashcardReviewLogRepository logRepo;
  private final Sm2Scheduler scheduler;

  public FlashcardReviewService(FlashcardRepository flashcardRepo,
                                FlashcardReviewRepository reviewRepo,
                                FlashcardReviewLogRepository logRepo,
                                FlashcardSchedulerProperties schedulerProperties) {
    this.flashcardRepo = flashcardRepo;
    this.reviewRepo = reviewRepo;
    this.logRepo = logRepo;
    this.scheduler = new Sm2Scheduler(
        schedulerProperties.getLearningStepsMinutes(),
        schedulerProperties.getEasyIntervalDays(),
        schedulerProperties.getReviewAgainRelearnMinutes()
    );
  }

  @Override
  @Transactional
  public RegisterResponse registerReview(RegisterCommand command) {
    if (command == null) throw new IllegalArgumentException("command cannot be null");
    if (command.userId() == null) throw new IllegalArgumentException("userId cannot be null");
    if (command.flashcardId() == null) throw new IllegalArgumentException("flashcardId cannot be null");
    if (command.grade() == null) throw new IllegalArgumentException("grade cannot be null");

    LocalDateTime reviewedAt = command.reviewedAt() != null ? command.reviewedAt() : LocalDateTime.now();

    flashcardRepo.findById(command.flashcardId())
        .orElseThrow(() -> new NoSuchElementException("Flashcard not found"));

    var existingLog = logRepo.findMostRecentByUserIdAndFlashcardIdAfter(
        command.userId(), command.flashcardId(), reviewedAt.minusSeconds(60));
    if (existingLog.isPresent()) {
      FlashcardReview existingReview = reviewRepo.findByUserIdAndFlashcardId(
              command.userId(), command.flashcardId())
          .orElseThrow(() -> new NoSuchElementException("Review not found"));
      return new RegisterResponse(existingReview, existingLog.get());
    }

    FlashcardReview review = reviewRepo.findByUserIdAndFlashcardId(command.userId(), command.flashcardId())
        .orElseGet(() -> FlashcardReview.createNew(command.userId(), command.flashcardId(), reviewedAt));

    Sm2Scheduler.Result result = scheduler.schedule(review, command.grade(), reviewedAt);

    review.update(
        result.getState(),
        result.getEaseAfter(),
        result.getIntervalAfter(),
        result.getLearningStepAfter(),
        result.getRepetitions(),
        result.getLapses(),
        result.getDueAt()
    );

    FlashcardReview saved = reviewRepo.save(review);

    FlashcardReviewLog log = FlashcardReviewLog.create(
        command.userId(),
        command.flashcardId(),
        command.grade(),
        reviewedAt,
        result.getIntervalBefore(),
        result.getIntervalAfter(),
        result.getEaseBefore(),
        result.getEaseAfter()
    );

    FlashcardReviewLog savedLog = logRepo.save(log);

    return new RegisterResponse(saved, savedLog);
  }
}
