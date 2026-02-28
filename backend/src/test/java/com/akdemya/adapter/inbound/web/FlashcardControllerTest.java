package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.FlashcardDto;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.FlashcardReviewLog;
import com.akdemya.domain.model.ReviewGrade;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.FlashcardReviewUseCase;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewLogRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FlashcardControllerTest {

  private final FlashcardStudyUseCase studyUseCase = mock(FlashcardStudyUseCase.class);
  private final FlashcardReviewUseCase reviewUseCase = mock(FlashcardReviewUseCase.class);
  private final FlashcardRepository flashcardRepo = mock(FlashcardRepository.class);
  private final FlashcardReviewRepository reviewRepo = mock(FlashcardReviewRepository.class);
  private final FlashcardReviewLogRepository reviewLogRepo = mock(FlashcardReviewLogRepository.class);
  private final UserRepository userRepo = mock(UserRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);

  private final FlashcardController controller = new FlashcardController(
      studyUseCase, reviewUseCase, flashcardRepo, reviewRepo, reviewLogRepo, userRepo, unitRepo);

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
}
