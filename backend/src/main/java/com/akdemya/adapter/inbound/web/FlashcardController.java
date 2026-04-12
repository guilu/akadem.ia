package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final FlashcardStudyUseCase studyUseCase;
  private final FlashcardReviewUseCase reviewUseCase;
  private final FlashcardManagementUseCase managementUseCase;
  private final FlashcardImportExportUseCase importExportUseCase;
  private final FlashcardRepository flashcardRepo;
  private final FlashcardReviewLogRepository reviewLogRepo;
  private final UserRepository userRepo;

  public FlashcardController(FlashcardStudyUseCase studyUseCase,
                             FlashcardReviewUseCase reviewUseCase,
                             FlashcardManagementUseCase managementUseCase,
                             FlashcardImportExportUseCase importExportUseCase,
                             FlashcardRepository flashcardRepo,
                             FlashcardReviewLogRepository reviewLogRepo,
                             UserRepository userRepo) {
    this.studyUseCase = studyUseCase;
    this.reviewUseCase = reviewUseCase;
    this.managementUseCase = managementUseCase;
    this.importExportUseCase = importExportUseCase;
    this.flashcardRepo = flashcardRepo;
    this.reviewLogRepo = reviewLogRepo;
    this.userRepo = userRepo;
  }

  @GetMapping
  public List<FlashcardDto.FlashcardResponse> listByUnit(@RequestParam UUID unitId,
                                                         @AuthenticationPrincipal User principal) {
    List<Flashcard> flashcards;
    if (principal == null) {
      // Unauthenticated — return only global flashcards
      flashcards = managementUseCase.listByUnit(unitId);
    } else {
      UUID userId = requireUserId(principal);
      flashcards = managementUseCase.listVisibleByUnit(unitId, userId);
    }
    return flashcards.stream()
        .map(this::toFlashcardResponse)
        .toList();
  }

  @PostMapping
  public ResponseEntity<FlashcardDto.FlashcardResponse> create(@RequestBody FlashcardDto.CreateRequest req,
                                                               @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    if (req == null || req.unitId() == null) {
      return ResponseEntity.badRequest().build();
    }
    Visibility visibility = req.visibility() != null ? req.visibility() : Visibility.PRIVATE;

    // Only ADMINs can create GLOBAL flashcards
    if (visibility == Visibility.GLOBAL && !isAdmin(principal)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    try {
      Flashcard saved = managementUseCase.createFlashcardWithVisibility(
          new FlashcardManagementUseCase.CreateCommandWithVisibility(
              req.unitId(), req.front(), req.back(), visibility, userId));
      return ResponseEntity.status(HttpStatus.CREATED).body(toFlashcardResponse(saved));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<FlashcardDto.FlashcardResponse> update(@PathVariable UUID id,
                                                               @RequestBody FlashcardDto.UpdateRequest req,
                                                               @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    boolean admin = isAdmin(principal);
    if (req == null) {
      return ResponseEntity.badRequest().build();
    }
    try {
      Flashcard saved = managementUseCase.updateFlashcardIfAuthorized(
          new FlashcardManagementUseCase.UpdateCommand(id, req.unitId(), req.front(), req.back()),
          userId, admin);
      return ResponseEntity.ok(toFlashcardResponse(saved));
    } catch (AccessDeniedException ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (NoSuchElementException ex) {
      return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    boolean admin = isAdmin(principal);
    try {
      managementUseCase.deleteFlashcardIfAuthorized(id, userId, admin);
      return ResponseEntity.noContent().build();
    } catch (AccessDeniedException ex) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (NoSuchElementException ex) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/study/queue")
  public ResponseEntity<FlashcardDto.StudyQueueResponse> getStudyQueue(@RequestParam(required = false) UUID unitId,
                                                                       @RequestParam(required = false) Integer limit,
                                                                       @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    int resolvedLimit = resolveLimit(limit);
    if (resolvedLimit < 0) {
      return ResponseEntity.badRequest().build();
    }
    var response = studyUseCase.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(
        userId, unitId, resolvedLimit, null));
    return ResponseEntity.ok(new FlashcardDto.StudyQueueResponse(
        response.newCount(), response.dueCount(), response.learningCount()
    ));
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
    return studyUseCase.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(userId, null))
        .stream()
        .map(r -> new FlashcardDto.UnitSummary(
            r.unitId(), r.unitName(), r.subjectId(), r.subjectName(),
            r.newCount(), r.reviewCount(), r.dueCount()))
        .toList();
  }

  @PostMapping(value = "/import", consumes = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<FlashcardDto.ImportResult> importFlashcards(
      @RequestParam UUID unitId,
      @RequestParam(defaultValue = "csv") String format,
      @RequestBody String content,
      @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    boolean admin = isAdmin(principal);
    FlashcardImportExportUseCase.ImportResult result =
        importExportUseCase.importFlashcards(unitId, format, content, userId, admin);
    return ResponseEntity.ok(
        new FlashcardDto.ImportResult(result.imported(), result.skipped(), result.errors()));
  }

  @GetMapping("/export")
  public ResponseEntity<String> exportFlashcards(
      @RequestParam(required = false) UUID unitId,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(defaultValue = "csv") String format,
      @AuthenticationPrincipal User principal) {
    UUID userId = requireUserId(principal);
    if (unitId == null && subjectId == null) {
      return ResponseEntity.badRequest().body("Se requiere unitId o subjectId");
    }
    boolean admin = isAdmin(principal);
    String content = unitId != null
        ? importExportUseCase.exportFlashcards(unitId, format, userId, admin)
        : importExportUseCase.exportFlashcardsBySubject(subjectId, userId, format);
    boolean isJson = "json".equalsIgnoreCase(format);
    String contentType = isJson ? "application/json; charset=UTF-8" : "text/csv; charset=UTF-8";
    String filename = isJson ? "flashcards.json" : "flashcards.csv";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, contentType);
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    return ResponseEntity.ok().headers(headers).body(content);
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

  private boolean isAdmin(User principal) {
    return principal != null && principal.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  private int resolveLimit(Integer limit) {
    int resolved = limit == null ? DEFAULT_LIMIT : limit;
    if (resolved < 0 || resolved > MAX_LIMIT) {
      return -1;
    }
    return resolved;
  }
}
