package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlashcardControllerTest {


  private final FlashcardImportExportUseCase importExportUseCase = mock(FlashcardImportExportUseCase.class);
  private final FlashcardStudyUseCase studyUseCase = mock(FlashcardStudyUseCase.class);
  private final FlashcardReviewUseCase reviewUseCase = mock(FlashcardReviewUseCase.class);
  private final FlashcardManagementUseCase managementUseCase = mock(FlashcardManagementUseCase.class);
  private final PrincipalResolver principalResolver = mock(PrincipalResolver.class);

  private final FlashcardController controller = new FlashcardController(
      studyUseCase,
       reviewUseCase,
       managementUseCase,
       importExportUseCase,
       principalResolver);

  @Test
  void studyQueueReturnsCounts() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    when(studyUseCase.getStudyQueue(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyQueueResponse(3, 2, 1));

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, 5, principal);

    assertEquals(200, response.getStatusCode().value());
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
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    FlashcardReview review = new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        ReviewState.LEARNING, 2.5, 3, 0, 2, 0, LocalDateTime.now(),
        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    FlashcardReviewLog log = FlashcardReviewLog.create(userId, flashcardId, ReviewGrade.GOOD,
        LocalDateTime.now(), 1, 3, 2.5, 2.6);
    when(reviewUseCase.registerReview(any()))
        .thenReturn(new FlashcardReviewUseCase.RegisterResponse(review, log));

    var request = new FlashcardDto.ReviewRequest(flashcardId, ReviewGrade.GOOD, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(request, principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().review().flashcardId());
  }

  @Test
  void unitSummaryReturnsNewAndReviewCounts() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    var summaryResult = new FlashcardStudyUseCase.UnitSummaryResult(
        unitId, "Unidad 1", subjectId, "Math", null, "Syllabus 1", 5L, 0L, 0L);
    when(studyUseCase.getUnitSummaries(any())).thenReturn(List.of(summaryResult));

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
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    var historyItem = new FlashcardReviewUseCase.HistoryItemResult(
        UUID.randomUUID(), flashcardId, "front", "back", ReviewGrade.HARD,
        LocalDateTime.now(), 1, 2, 2.5, 2.4);
    when(reviewUseCase.getReviewHistory(eq(userId), eq(20))).thenReturn(List.of(historyItem));

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(null, principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    verify(reviewUseCase).getReviewHistory(userId, 20);
  }

  // --- export endpoint ---

  @Test
  void exportBySubjectIdCsvReturnsContent() {
    UUID userId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(importExportUseCase.exportFlashcardsBySubject(subjectId, userId, "csv"))
        .thenReturn("front,back\nHello,Hola\n");

    ResponseEntity<String> response = controller.exportFlashcards(null, subjectId, "csv", principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertTrue(response.getBody().contains("Hello,Hola"));
    verify(importExportUseCase).exportFlashcardsBySubject(subjectId, userId, "csv");
  }

  @Test
  void exportBySubjectIdJsonReturnsContent() {
    UUID userId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(importExportUseCase.exportFlashcardsBySubject(subjectId, userId, "json"))
        .thenReturn("[{\"front\":\"Hello\",\"back\":\"Hola\"}]");

    ResponseEntity<String> response = controller.exportFlashcards(null, subjectId, "json", principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    org.junit.jupiter.api.Assertions.assertTrue(response.getBody().contains("Hello"));
    verify(importExportUseCase).exportFlashcardsBySubject(subjectId, userId, "json");
  }

  @Test
  void exportWithNeitherParamReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    ResponseEntity<String> response = controller.exportFlashcards(null, null, "csv", principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void exportByUnitIdStillWorksWhenUnitIdProvided() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(importExportUseCase.exportFlashcards(eq(unitId), eq("csv"), eq(userId), eq(false)))
        .thenReturn("front,back\nQ,A\n");

    ResponseEntity<String> response = controller.exportFlashcards(unitId, null, "csv", principal);

    assertEquals(200, response.getStatusCode().value());
    verify(importExportUseCase).exportFlashcards(eq(unitId), eq("csv"), eq(userId), eq(false));
  }

  @Test
  void createWithoutAuthReturns401() {
    when(principalResolver.requireUserId(isNull())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back", com.akdemya.domain.model.Visibility.PRIVATE);
    assertThrows(ResponseStatusException.class, () -> controller.create(req, null));
  }

  @Test
  void updateWithoutAuthReturns401() {
    when(principalResolver.requireUserId(isNull())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
    var req = new FlashcardDto.UpdateRequest(null, "front", "back");
    assertThrows(ResponseStatusException.class, () -> controller.update(UUID.randomUUID(), req, null));
  }

  @Test
  void deleteWithoutAuthReturns401() {
    when(principalResolver.requireUserId(isNull())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
    assertThrows(ResponseStatusException.class, () -> controller.delete(UUID.randomUUID(), null));
  }

  // --- create ---

  @Test
  void createReturnsSavedFlashcard() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    Flashcard saved = new Flashcard(flashcardId, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(saved);

    var req = new FlashcardDto.CreateRequest(unitId, "front", "back", null);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(201, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().id());
  }

  @Test
  void createWithNullUnitIdReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    var req = new FlashcardDto.CreateRequest(null, "front", "back", null);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void createWithIllegalArgumentReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(managementUseCase.createFlashcardWithVisibility(any())).thenThrow(new IllegalArgumentException("invalid"));

    var req = new FlashcardDto.CreateRequest(UUID.randomUUID(), "front", "back", null);
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.create(req, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  // --- update ---

  @Test
  void updateReturnsUpdatedFlashcard() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    Flashcard updated = new Flashcard(flashcardId, unitId, "front2", "back2",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.updateFlashcardIfAuthorized(any(), any(), anyBoolean())).thenReturn(updated);

    var req = new FlashcardDto.UpdateRequest(unitId, "front2", "back2");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(flashcardId, req, principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().id());
  }

  @Test
  void updateWithNullRequestReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), null, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void updateNotFoundReturns404() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(managementUseCase.updateFlashcardIfAuthorized(any(), any(), anyBoolean()))
        .thenThrow(new java.util.NoSuchElementException());

    var req = new FlashcardDto.UpdateRequest(UUID.randomUUID(), "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  void updateWithIllegalArgumentReturns400() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(managementUseCase.updateFlashcardIfAuthorized(any(), any(), anyBoolean()))
        .thenThrow(new IllegalArgumentException("invalid"));

    var req = new FlashcardDto.UpdateRequest(UUID.randomUUID(), "front", "back");
    ResponseEntity<FlashcardDto.FlashcardResponse> response = controller.update(UUID.randomUUID(), req, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  // --- delete ---

  @Test
  void deleteReturns204() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    doNothing().when(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), any(), anyBoolean());

    ResponseEntity<Void> response = controller.delete(flashcardId, principal);

    assertEquals(204, response.getStatusCode().value());
    verify(managementUseCase).deleteFlashcardIfAuthorized(eq(flashcardId), any(), anyBoolean());
  }

  // --- listByUnit ---

  @Test
  void listByUnitReturnsFlashcards() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    Flashcard card = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now());
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    List<FlashcardDto.FlashcardResponse> response = controller.listByUnit(unitId, principal);

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
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    when(studyUseCase.getStudyNext(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyNextResponse(
            flashcardId, unitId, "front", "back", ReviewState.NEW, null,
            new FlashcardStudyUseCase.IntervalHints("1m", "10m", "4d")));

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(flashcardId, response.getBody().flashcardId());
    assertNotNull(response.getBody().intervalHints());
  }

  @Test
  void studyNextReturns204WhenNoCards() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);
    when(studyUseCase.getStudyNext(any())).thenReturn(null);

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(204, response.getStatusCode().value());
  }

  @Test
  void studyNextWithNullIntervalHintsStillReturns200() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    when(studyUseCase.getStudyNext(any()))
        .thenReturn(new FlashcardStudyUseCase.StudyNextResponse(
            flashcardId, unitId, "front", "back", ReviewState.NEW, null, null));

    ResponseEntity<FlashcardDto.StudyNextResponse> response = controller.getStudyNext(unitId, principal);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertNull(response.getBody().intervalHints());
  }

  // --- getDashboard ---

  @Test
  void dashboardReturnsAggregatedData() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    when(studyUseCase.getDashboard(any()))
        .thenReturn(new FlashcardStudyUseCase.DashboardResponse(3, 2, 1, 0, 5, 6));

    ResponseEntity<FlashcardDto.DashboardResponse> response = controller.getDashboard(unitId, principal);

    assertEquals(200, response.getStatusCode().value());
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
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    when(importExportUseCase.importFlashcards(eq(unitId), eq("csv"), eq("front,back\nHello,Hola\n"), eq(userId), eq(false)))
        .thenReturn(new com.akdemya.domain.port.in.FlashcardImportExportUseCase.ImportResult(1, 0, List.of()));

    ResponseEntity<FlashcardDto.ImportResult> response =
        controller.importFlashcards(unitId, "csv", "front,back\nHello,Hola\n", principal);

    assertEquals(200, response.getStatusCode().value());
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
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, -1, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void studyQueueReturns400WhenLimitExceedsMax() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    ResponseEntity<FlashcardDto.StudyQueueResponse> response =
        controller.getStudyQueue(unitId, 101, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  // --- history edge cases ---

  @Test
  void historyReturns400WhenLimitIsNegative() {
    UUID userId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(-1, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void historyWithNullFlashcardInMapReturnsNullFrontAndBack() {
    UUID userId = UUID.randomUUID();
    UUID flashcardId = UUID.randomUUID();
    var principal = new User("user@example.com", "", List.of());
    when(principalResolver.requireUserId(any())).thenReturn(userId);

    var historyItem = new FlashcardReviewUseCase.HistoryItemResult(
        UUID.randomUUID(), flashcardId, null, null, ReviewGrade.GOOD,
        LocalDateTime.now(), 1, 3, 2.5, 2.6);
    when(reviewUseCase.getReviewHistory(eq(userId), eq(20))).thenReturn(List.of(historyItem));

    ResponseEntity<List<FlashcardDto.HistoryItem>> response = controller.getHistory(null, principal);

    assertEquals(200, response.getStatusCode().value());
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

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void registerReviewWithNullFlashcardIdReturns400() {
    var principal = new User("user@example.com", "", List.of());

    var req = new FlashcardDto.ReviewRequest(null, ReviewGrade.GOOD, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(req, principal);

    assertEquals(400, response.getStatusCode().value());
  }

  @Test
  void registerReviewWithNullGradeReturns400() {
    var principal = new User("user@example.com", "", List.of());

    var req = new FlashcardDto.ReviewRequest(UUID.randomUUID(), null, LocalDateTime.now());
    ResponseEntity<FlashcardDto.ReviewResponse> response = controller.registerReview(req, principal);

    assertEquals(400, response.getStatusCode().value());
  }
}
