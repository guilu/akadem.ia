package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlashcardControllerTest {


  private final FlashcardImportExportUseCase importExportUseCase = mock(FlashcardImportExportUseCase.class);
  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);

  private final FlashcardStudyUseCase studyUseCase = mock(FlashcardStudyUseCase.class);
  private final FlashcardReviewUseCase reviewUseCase = mock(FlashcardReviewUseCase.class);
  private final FlashcardManagementUseCase managementUseCase = mock(FlashcardManagementUseCase.class);
  private final FlashcardRepository flashcardRepo = mock(FlashcardRepository.class);
  private final FlashcardReviewRepository reviewRepo = mock(FlashcardReviewRepository.class);
  private final FlashcardReviewLogRepository reviewLogRepo = mock(FlashcardReviewLogRepository.class);
  private final UserRepository userRepo = mock(UserRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);

  private final FlashcardController controller = new FlashcardController(
      studyUseCase,
       reviewUseCase,
       managementUseCase,
       importExportUseCase,
       flashcardRepo,
       reviewRepo,
       reviewLogRepo,
       userRepo,
       unitRepo,
       subjectRepo);

  @Test
  void studyQueueReturnsCounts() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    when(studyUseCase.getStudyQueue(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyQueueResponse(3, 2, 1));

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, 5, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(3, response.getBody().newCount());
    assertEquals(2, response.getBody().dueCount());
    assertEquals(1, response.getBody().learningCount());
  }

  @Test
  void registerReviewReturnsReviewPayload() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    FlashcardReview review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 3, 0, 2, 0, LocalDateTime.now(),
        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    FlashcardReviewLog log = FlashcardReviewLog.create(userId, flashcardId, ReviewGrade.GOOD,
        LocalDateTime.now(), 1, 3, 2.5, 2.6);
    when(reviewUseCase.registerReview(any()))
        .thenReturn(new FlashcardReviewUseCase.RegisterResponse(review, log));

    var request = new FlashcardDto.ReviewRequest(flashcardId, ReviewGrade.GOOD, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(request, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().review().flashcardId());
  }

  @Test
  void unitSummaryReturnsNewAndReviewCounts() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    Unit unit = new Unit(unitId, UUID.randomUUID(), "Unidad 1", "", 1);
    when(unitRepo.findAllWithFlashcards()).thenReturn(List.of(unit));
    when(flashcardRepo.countNewByUserIdAndUnitId(userId, unitId)).thenReturn(5L);
    when(reviewRepo.countByUserIdAndUnitIdAndStateIn(eq(userId), eq(unitId), anyList())).thenReturn(0L);
    when(reviewRepo.countDueByUserIdAndUnitIdUpTo(eq(userId), eq(unitId), any())).thenReturn(0L);

    List<FlashcardDto.UnitSummary> response = controller.getUnitSummary(principal);

    assertEquals(1, response.size());
    assertEquals(5L, response.get(0).newCount());
    assertEquals(0L, response.get(0).reviewCount());
    assertEquals(0L, response.get(0).dueCount());
  }

  @Test
  void historyDefaultsToLimitTwenty() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    FlashcardReviewLog log = FlashcardReviewLog.create(userId, flashcardId, ReviewGrade.HARD,
        LocalDateTime.now(), 1, 2, 2.5, 2.4);
    when(reviewLogRepo.findRecentByUserId(eq(userId), eq(20))).thenReturn(List.of(log));
    when(flashcardRepo.findByIds(any())).thenReturn(List.of(
        new Flashcard(flashcardId, UUID.randomUUID(), "front", "back",
            LocalDateTime.now(), LocalDateTime.now())
    ));

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(null, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    verify(reviewLogRepo).findRecentByUserId(userId, 20);
  }

  // --- export endpoint ---

  @Test
  void exportBySubjectIdCsvReturnsContent() {
    UUID userId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(importExportUseCase.exportFlashcardsBySubject(subjectId, "csv"))
        .thenReturn("front,back\nHello,Hola\n");

    ResponseEntity<String> response = controller.exportFlashcards(null, subjectId, "csv", principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertTrue(response.getBody().contains("Hello,Hola"));
    verify(importExportUseCase).exportFlashcardsBySubject(subjectId, "csv");
  }

  @Test
  void exportBySubjectIdJsonReturnsContent() {
    UUID userId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(importExportUseCase.exportFlashcardsBySubject(subjectId, "json"))
        .thenReturn("[{\"front\":\"Hello\",\"back\":\"Hola\"}]");

    ResponseEntity<String> response = controller.exportFlashcards(null, subjectId, "json", principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertTrue(response.getBody().contains("Hello"));
    verify(importExportUseCase).exportFlashcardsBySubject(subjectId, "json");
  }

  @Test
  void exportWithNeitherParamReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    ResponseEntity<String> response = controller.exportFlashcards(null, null, "csv", principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void exportByUnitIdStillWorksWhenUnitIdProvided() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(importExportUseCase.exportFlashcards(unitId, "csv"))
        .thenReturn("front,back\nQ,A\n");

    ResponseEntity<String> response = controller.exportFlashcards(unitId, null, "csv", principal);

    assertEquals(200, response.getStatusCodeValue());
    verify(importExportUseCase).exportFlashcards(unitId, "csv");
  }

  @Test
  void createWithoutAuthReturns401() {
    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back");
    assertThrows(ResponseStatusException.class, () -> controller.create(req, null));
  }

  @Test
  void updateWithoutAuthReturns401() {
    var req = new FlashcardDto.UpdateRequest(null, "front", "back");
    assertThrows(ResponseStatusException.class, () -> controller.update(UUID.randomUUID(), req, null));
  }

  @Test
  void deleteWithoutAuthReturns401() {
    assertThrows(ResponseStatusException.class, () -> controller.delete(UUID.randomUUID(), null));
  }

  // --- create ---

  @Test
  void createReturnsSavedFlashcard() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    Flashcard saved = new Flashcard(flashcardId, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.createFlashcard(any())).thenReturn(saved);

    var req = new FlashcardDto.CreateRequest(unitId, "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(201, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().id());
  }

  @Test
  void createWithNullUnitIdReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    var req = new FlashcardDto.CreateRequest(null, "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void createWithIllegalArgumentReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(managementUseCase.createFlashcard(any())).thenThrow(new IllegalArgumentException("invalid"));

    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  // --- update ---

  @Test
  void updateReturnsUpdatedFlashcard() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    Flashcard updated = new Flashcard(flashcardId, unitId, "front2", "back2",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.updateFlashcard(any())).thenReturn(updated);

    var req = new FlashcardDto.UpdateRequest(unitId, "front2", "back2");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().id());
  }

  @Test
  void updateWithNullRequestReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), null, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void updateNotFoundReturns404() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(managementUseCase.updateFlashcard(any())).thenThrow(new java.util.NoSuchElementException());

    var req = new FlashcardDto.UpdateRequest(UUID.randomUUID(), "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(404, response.getStatusCodeValue());
  }

  @Test
  void updateWithIllegalArgumentReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(managementUseCase.updateFlashcard(any())).thenThrow(new IllegalArgumentException("invalid"));

    var req = new FlashcardDto.UpdateRequest(UUID.randomUUID(), "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  // --- delete ---

  @Test
  void deleteReturns204() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    doNothing().when(managementUseCase).deleteFlashcard(flashcardId);

    ResponseEntity<Void> response = controller.delete(flashcardId, principal);

    assertEquals(204, response.getStatusCodeValue());
    verify(managementUseCase).deleteFlashcard(flashcardId);
  }

  // --- listByUnit ---

  @Test
  void listByUnitReturnsFlashcards() {
    UUID unitId = UUID.randomUUID();
    Flashcard card = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.listByUnit(unitId)).thenReturn(List.of(card));

    List<FlashcardDto.FlashcardResponse> response = controller.listByUnit(unitId);

    assertEquals(1, response.size());
    assertEquals("front", response.get(0).front());
  }

  // --- getStudyNext ---

  @Test
  void studyNextReturnsFlashcard() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    when(studyUseCase.getStudyNext(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyNextResponse(
            flashcardId, unitId, "front", "back", ReviewState.NEW, null,
            new FlashcardStudyUseCase.IntervalHints("1m", "10m", "4d")));

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().flashcardId());
    assertNotNull(response.getBody().intervalHints());
  }

  @Test
  void studyNextReturns204WhenNoCards() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));
    when(studyUseCase.getStudyNext(any())).thenReturn(null);

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(204, response.getStatusCodeValue());
  }

  @Test
  void studyNextWithNullIntervalHintsStillReturns200() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    when(studyUseCase.getStudyNext(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyNextResponse(
            flashcardId, unitId, "front", "back", ReviewState.NEW, null, null));

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertNull(response.getBody().intervalHints());
  }

  // --- getDashboard ---

  @Test
  void dashboardReturnsAggregatedData() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    when(studyUseCase.getDashboard(any()))
        .thenReturn(new FlashcardStudyUseCase.DashboardResponse(3, 2, 1, 0, 5, 6));

    ResponseEntity<FlashcardDto.DashboardResponse> response = controller.getDashboard(unitId, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(3, response.getBody().dueToday());
    assertEquals(5, response.getBody().newCards());
  }

  // --- importFlashcards ---

  @Test
  void importFlashcardsReturnsImportResult() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    when(importExportUseCase.importFlashcards(unitId, "csv", "front,back\nHello,Hola\n"))
        .thenReturn(new com.akdemya.domain.port.in.FlashcardImportExportUseCase.ImportResult(1, 0, List.of()));

    ResponseEntity<FlashcardDto.ImportResult> response =
        controller.importFlashcards(unitId, "csv", "front,back\nHello,Hola\n", principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().imported());
    assertEquals(0, response.getBody().skipped());
  }

  // --- studyQueue edge cases ---

  @Test
  void studyQueueReturns400WhenLimitIsNegative() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, -1, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void studyQueueReturns400WhenLimitExceedsMax() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, 101, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  // --- history edge cases ---

  @Test
  void historyReturns400WhenLimitIsNegative() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(-1, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void historyWithNullFlashcardInMapReturnsNullFrontAndBack() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(userRepo.findByEmail("user@example.com"))
        .thenReturn(Optional.of(new AppUser(userId, "user@example.com", "", "USER", null, null, null)));

    FlashcardReviewLog log = FlashcardReviewLog.create(userId, flashcardId, ReviewGrade.GOOD,
        LocalDateTime.now(), 1, 3, 2.5, 2.6);
    when(reviewLogRepo.findRecentByUserId(eq(userId), eq(20))).thenReturn(List.of(log));
    when(flashcardRepo.findByIds(any())).thenReturn(List.of()); // no matching flashcard

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(null, principal);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertNull(response.getBody().get(0).front());
    assertNull(response.getBody().get(0).back());
  }

  // --- reviewRequest edge cases ---

  @Test
  void registerReviewWithNullRequestReturns400() {
    var principal = new User("user@example.com", "", List.of());

    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(null, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void registerReviewWithNullFlashcardIdReturns400() {
    var principal = new User("user@example.com", "", List.of());

    var req = new FlashcardDto.ReviewRequest(null, ReviewGrade.GOOD, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }

  @Test
  void registerReviewWithNullGradeReturns400() {
    var principal = new User("user@example.com", "", List.of());

    var req = new FlashcardDto.ReviewRequest(UUID.randomUUID(), null, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(req, principal);

    assertEquals(400, response.getStatusCodeValue());
  }
}
