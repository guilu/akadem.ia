package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flashcards")
@CrossOrigin(origins = "*")
public class FlashcardController {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final FlashcardStudyUseCase studyUseCase;
  private final FlashcardReviewUseCase reviewUseCase;
  private final FlashcardRepository flashcardRepo;
  private final FlashcardReviewRepository reviewRepo;
  private final FlashcardReviewLogRepository reviewLogRepo;
  private final UserRepository userRepo;
  private final UnitRepository unitRepo;

  public FlashcardController(FlashcardStudyUseCase studyUseCase,
                             FlashcardReviewUseCase reviewUseCase,
                             FlashcardRepository flashcardRepo,
                             FlashcardReviewRepository reviewRepo,
                             FlashcardReviewLogRepository reviewLogRepo,
                             UserRepository userRepo,
                             UnitRepository unitRepo) {
    this.studyUseCase = studyUseCase;
    this.reviewUseCase = reviewUseCase;
    this.flashcardRepo = flashcardRepo;
    this.reviewRepo = reviewRepo;
    this.reviewLogRepo = reviewLogRepo;
    this.userRepo = userRepo;
    this.unitRepo = unitRepo;
  }

  @GetMapping
  public List<FlashcardDto.FlashcardResponse> listByUnit(@RequestParam UUID unitId) {
    return flashcardRepo.findByUnitId(unitId).stream()
        .map(this::toFlashcardResponse)
        .toList();
  }

  @PostMapping
  public ResponseEntity<FlashcardDto.FlashcardResponse> create(@RequestBody FlashcardDto.CreateRequest req) {
    if (req == null || req.unitId() == null) {
      return ResponseEntity.badRequest().build();
    }
    try {
      Flashcard created = Flashcard.create(req.unitId(), req.front(), req.back());
      Flashcard saved = flashcardRepo.save(created);
      return ResponseEntity.status(HttpStatus.CREATED).body(toFlashcardResponse(saved));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<FlashcardDto.FlashcardResponse> update(@PathVariable UUID id,
                                                               @RequestBody FlashcardDto.UpdateRequest req) {
    if (req == null) {
      return ResponseEntity.badRequest().build();
    }
    Optional<Flashcard> existingOpt = flashcardRepo.findById(id);
    if (existingOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Flashcard existing = existingOpt.get();
    UUID unitId = req.unitId() != null ? req.unitId() : existing.getUnitId();
    String front = req.front() != null ? req.front() : existing.getFront();
    String back = req.back() != null ? req.back() : existing.getBack();
    try {
      Flashcard updated = new Flashcard(existing.getId(), unitId, front, back,
          existing.getCreatedAt(), LocalDateTime.now());
      Flashcard saved = flashcardRepo.save(updated);
      return ResponseEntity.ok(toFlashcardResponse(saved));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    flashcardRepo.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/study/queue")
  public ResponseEntity<FlashcardDto.StudyQueueResponse> getStudyQueue(@RequestParam UUID unitId,
                                                                       @RequestParam(required = false) Integer limit,
                                                                       @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    int resolvedLimit = resolveLimit(limit);
    if (resolvedLimit < 0) {
      return ResponseEntity.badRequest().build();
    }
    var response = studyUseCase.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(
        userId, unitId, resolvedLimit, null));
    var items = response.items().stream()
        .map(item -> new FlashcardDto.StudyQueueItem(
            item.flashcardId(), item.front(), item.back(), item.state(), item.dueAt()))
        .toList();
    return ResponseEntity.ok(new FlashcardDto.StudyQueueResponse(items));
  }

  @GetMapping("/study/next")
  public ResponseEntity<FlashcardDto.StudyNextResponse> getStudyNext(@RequestParam UUID unitId,
                                                                     @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    var response = studyUseCase.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(
        userId, unitId, null));
    if (response == null) {
      return ResponseEntity.noContent().build();
    }
    FlashcardDto.IntervalHints hints = response.intervalHints() != null
        ? new FlashcardDto.IntervalHints(
            response.intervalHints().again(),
            response.intervalHints().good(),
            response.intervalHints().easy())
        : null;
    return ResponseEntity.ok(new FlashcardDto.StudyNextResponse(
        response.flashcardId(), response.unitId(), response.front(), response.back(),
        response.state(), response.dueAt(), hints
    ));
  }

  @PostMapping("/study/review")
  public ResponseEntity<FlashcardDto.ReviewResponse> registerReview(@RequestBody FlashcardDto.ReviewRequest req,
                                                                    @AuthenticationPrincipal User principal) {
    if (req == null || req.flashcardId() == null || req.grade() == null) {
      return ResponseEntity.badRequest().build();
    }
    UUID userId = requireUserId(principal);
    var response = reviewUseCase.registerReview(new FlashcardReviewUseCase.RegisterCommand(
        userId, req.flashcardId(), req.grade(), req.reviewedAt()));
    return ResponseEntity.ok(toReviewResponse(response));
  }

  @GetMapping("/dashboard")
  public ResponseEntity<FlashcardDto.DashboardResponse> getDashboard(@RequestParam UUID unitId,
                                                                     @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    var response = studyUseCase.getDashboard(new FlashcardStudyUseCase.DashboardCommand(
        userId, unitId, null));
    return ResponseEntity.ok(new FlashcardDto.DashboardResponse(
        response.dueToday(),
        response.dueIn1to3Days(),
        response.dueIn4to7Days(),
        response.dueIn8to30Days(),
        response.newCards(),
        response.totalDue()
    ));
  }

  @GetMapping("/units/summary")
  public List<FlashcardDto.UnitSummary> getUnitSummary(@AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    LocalDateTime now = LocalDateTime.now();
    return unitRepo.findAllWithFlashcards().stream()
        .map(unit -> {
          long newCount = flashcardRepo.countNewByUserIdAndUnitId(userId, unit.getId());
          long reviewCount = reviewRepo.countByUserIdAndUnitIdAndStateIn(userId, unit.getId(),
              List.of(ReviewState.LEARNING, ReviewState.REVIEW));
          long dueCount = reviewRepo.countDueByUserIdAndUnitIdUpTo(userId, unit.getId(), now);
          return new FlashcardDto.UnitSummary(unit.getId(), unit.getName(), newCount, reviewCount, dueCount);
        })
        .toList();
  }

  @GetMapping("/history")
  public ResponseEntity<List<FlashcardDto.HistoryItem>> getHistory(
      @RequestParam(required = false) Integer limit,
      @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    int resolvedLimit = resolveLimit(limit);
    if (resolvedLimit < 0) {
      return ResponseEntity.badRequest().build();
    }
    List<FlashcardReviewLog> logs = reviewLogRepo.findRecentByUserId(userId, resolvedLimit);
    Map<UUID, Flashcard> flashcards = flashcardRepo.findByIds(
            logs.stream().map(FlashcardReviewLog::getFlashcardId).toList())
        .stream().collect(Collectors.toMap(Flashcard::getId, f -> f));
    List<FlashcardDto.HistoryItem> items = logs.stream()
        .map(log -> {
          Flashcard card = flashcards.get(log.getFlashcardId());
          return new FlashcardDto.HistoryItem(
              log.getId(),
              log.getFlashcardId(),
              card != null ? card.getFront() : null,
              card != null ? card.getBack() : null,
              log.getGrade(),
              log.getReviewedAt(),
              log.getIntervalBefore(),
              log.getIntervalAfter(),
              log.getEaseBefore(),
              log.getEaseAfter()
          );
        })
        .toList();
    return ResponseEntity.ok(items);
  }

  private FlashcardDto.ReviewResponse toReviewResponse(FlashcardReviewUseCase.RegisterResponse response) {
    FlashcardReview review = response.review();
    FlashcardReviewLog log = response.log();
    return new FlashcardDto.ReviewResponse(
        new FlashcardDto.Review(
            review.getId(), review.getUserId(), review.getFlashcardId(), review.getState(),
            review.getEaseFactor(), review.getIntervalDays(), review.getLearningStep(),
            review.getRepetitions(), review.getLapses(), review.getDueAt(), review.getLastReviewedAt()),
        new FlashcardDto.ReviewLog(
            log.getId(), log.getFlashcardId(), log.getGrade(), log.getReviewedAt(),
            log.getIntervalBefore(), log.getIntervalAfter(), log.getEaseBefore(), log.getEaseAfter())
    );
  }

  private FlashcardDto.FlashcardResponse toFlashcardResponse(Flashcard flashcard) {
    return new FlashcardDto.FlashcardResponse(
        flashcard.getId(),
        flashcard.getUnitId(),
        flashcard.getFront(),
        flashcard.getBack(),
        flashcard.getCreatedAt(),
        flashcard.getUpdatedAt()
    );
  }

  private UUID requireUserId(User principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return userRepo.findByEmail(principal.getUsername())
        .map(AppUser::getId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  private int resolveLimit(Integer limit) {
    int resolved = limit == null ? DEFAULT_LIMIT : limit;
    if (resolved < 0 || resolved > MAX_LIMIT) {
      return -1;
    }
    return resolved;
  }
}
