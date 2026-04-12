package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlashcardStudyServiceUnitSummaryTest {

  private final FlashcardRepository flashcardRepo = mock(FlashcardRepository.class);
  private final FlashcardReviewRepository reviewRepo = mock(FlashcardReviewRepository.class);
  private final UserSettingsRepository settingsRepo = mock(UserSettingsRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final FlashcardSchedulerProperties schedulerProperties = new FlashcardSchedulerProperties();

  private FlashcardStudyService service;

  private final UUID userId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.of(2026, 3, 1, 10, 0);

  @BeforeEach
  void setUp() {
    service = new FlashcardStudyService(
        flashcardRepo, reviewRepo, settingsRepo, unitRepo, subjectRepo, schedulerProperties);
  }

  @Test
  void getUnitSummaries_callsEachAggregateQueryExactlyOnce() {
    UUID subjectId = UUID.randomUUID();
    Unit unit = Unit.create(subjectId, "Unit A", "desc", 1);
    Subject subject = Subject.createGlobal("Math", "desc");

    when(unitRepo.findVisibleWithFlashcardsByUserId(userId)).thenReturn(List.of(unit));
    when(subjectRepo.findVisibleByUserId(userId)).thenReturn(List.of(subject));
    when(flashcardRepo.countNewByUserIdGroupByUnit(userId)).thenReturn(Map.of());
    when(reviewRepo.countByUserIdAndStateInGroupByUnit(eq(userId), any())).thenReturn(Map.of());
    when(reviewRepo.countDueByUserIdUpToGroupByUnit(eq(userId), any())).thenReturn(Map.of());

    service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(userId, now));

    verify(flashcardRepo, times(1)).countNewByUserIdGroupByUnit(userId);
    verify(reviewRepo, times(1)).countByUserIdAndStateInGroupByUnit(eq(userId), any());
    verify(reviewRepo, times(1)).countDueByUserIdUpToGroupByUnit(eq(userId), any());
    verify(unitRepo, times(1)).findVisibleWithFlashcardsByUserId(userId);
    verify(subjectRepo, times(1)).findVisibleByUserId(userId);
    verify(unitRepo, never()).findAllWithFlashcards();
    verify(subjectRepo, never()).findAll();
  }

  @Test
  void getUnitSummaries_assembliesCountsCorrectly() {
    UUID subjectId = UUID.randomUUID();
    Unit unit1 = Unit.create(subjectId, "Unit 1", "desc", 1);
    Unit unit2 = Unit.create(subjectId, "Unit 2", "desc", 2);
    Subject subject = new Subject(subjectId, "Math", "desc");

    when(unitRepo.findVisibleWithFlashcardsByUserId(userId)).thenReturn(List.of(unit1, unit2));
    when(subjectRepo.findVisibleByUserId(userId)).thenReturn(List.of(subject));
    when(flashcardRepo.countNewByUserIdGroupByUnit(userId))
        .thenReturn(Map.of(unit1.getId(), 5L));
    when(reviewRepo.countByUserIdAndStateInGroupByUnit(eq(userId), any()))
        .thenReturn(Map.of(unit1.getId(), 3L, unit2.getId(), 2L));
    when(reviewRepo.countDueByUserIdUpToGroupByUnit(eq(userId), any()))
        .thenReturn(Map.of(unit2.getId(), 4L));

    List<FlashcardStudyUseCase.UnitSummaryResult> results =
        service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(userId, now));

    assertEquals(2, results.size());

    FlashcardStudyUseCase.UnitSummaryResult r1 = results.stream()
        .filter(r -> r.unitId().equals(unit1.getId())).findFirst().orElseThrow();
    assertEquals(5L, r1.newCount());
    assertEquals(3L, r1.reviewCount());
    assertEquals(0L, r1.dueCount()); // no entry in dueMap → default 0
    assertEquals("Math", r1.subjectName());

    FlashcardStudyUseCase.UnitSummaryResult r2 = results.stream()
        .filter(r -> r.unitId().equals(unit2.getId())).findFirst().orElseThrow();
    assertEquals(0L, r2.newCount()); // no entry in newMap → default 0
    assertEquals(2L, r2.reviewCount());
    assertEquals(4L, r2.dueCount());
  }

  @Test
  void getUnitSummaries_returnsZeroCountsForUnitsNotInAnyMap() {
    UUID subjectId = UUID.randomUUID();
    Unit unit = Unit.create(subjectId, "Empty Unit", "desc", 1);
    Subject subject = new Subject(subjectId, "Science", "desc");

    when(unitRepo.findVisibleWithFlashcardsByUserId(userId)).thenReturn(List.of(unit));
    when(subjectRepo.findVisibleByUserId(userId)).thenReturn(List.of(subject));
    when(flashcardRepo.countNewByUserIdGroupByUnit(userId)).thenReturn(Map.of());
    when(reviewRepo.countByUserIdAndStateInGroupByUnit(eq(userId), any())).thenReturn(Map.of());
    when(reviewRepo.countDueByUserIdUpToGroupByUnit(eq(userId), any())).thenReturn(Map.of());

    List<FlashcardStudyUseCase.UnitSummaryResult> results =
        service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(userId, now));

    assertEquals(1, results.size());
    FlashcardStudyUseCase.UnitSummaryResult r = results.get(0);
    assertEquals(0L, r.newCount());
    assertEquals(0L, r.reviewCount());
    assertEquals(0L, r.dueCount());
    assertEquals("Science", r.subjectName());
    assertEquals(unit.getId(), r.unitId());
    assertEquals("Empty Unit", r.unitName());
  }

  @Test
  void getUnitSummaries_excludesOtherUsersPrivateSubjectsAndUnits() {
    UUID callerId = userId;
    UUID ownSubjectId = UUID.randomUUID();
    UUID globalSubjectId = UUID.randomUUID();
    UUID otherSubjectId = UUID.randomUUID();

    // Caller's own PRIVATE unit and a GLOBAL unit — both visible
    Unit ownUnit = Unit.create(ownSubjectId, "My Private Unit", "desc", 1);
    Unit globalUnit = Unit.create(globalSubjectId, "Global Unit", "desc", 1);

    // Only visible subjects/units are returned by the scoped repository methods
    Subject ownSubject = new Subject(ownSubjectId, "My Subject", "desc");
    Subject globalSubject = new Subject(globalSubjectId, "Global Subject", "desc");

    when(unitRepo.findVisibleWithFlashcardsByUserId(callerId))
        .thenReturn(List.of(ownUnit, globalUnit));  // other user's private unit NOT included
    when(subjectRepo.findVisibleByUserId(callerId))
        .thenReturn(List.of(ownSubject, globalSubject));  // other user's private subject NOT included
    when(flashcardRepo.countNewByUserIdGroupByUnit(callerId)).thenReturn(Map.of());
    when(reviewRepo.countByUserIdAndStateInGroupByUnit(eq(callerId), any())).thenReturn(Map.of());
    when(reviewRepo.countDueByUserIdUpToGroupByUnit(eq(callerId), any())).thenReturn(Map.of());

    List<FlashcardStudyUseCase.UnitSummaryResult> results =
        service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(callerId, now));

    assertEquals(2, results.size());
    assertTrue(results.stream().noneMatch(r -> r.subjectId().equals(otherSubjectId)),
        "Other user's private subject must not appear in results");
    assertTrue(results.stream().anyMatch(r -> r.subjectId().equals(ownSubjectId)));
    assertTrue(results.stream().anyMatch(r -> r.subjectId().equals(globalSubjectId)));
  }

  @Test
  void getUnitSummaries_onlyCallsVisibilityScopedRepositoryMethods() {
    when(unitRepo.findVisibleWithFlashcardsByUserId(userId)).thenReturn(List.of());
    when(subjectRepo.findVisibleByUserId(userId)).thenReturn(List.of());
    when(flashcardRepo.countNewByUserIdGroupByUnit(userId)).thenReturn(Map.of());
    when(reviewRepo.countByUserIdAndStateInGroupByUnit(eq(userId), any())).thenReturn(Map.of());
    when(reviewRepo.countDueByUserIdUpToGroupByUnit(eq(userId), any())).thenReturn(Map.of());

    service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(userId, now));

    // Must NEVER call the unscoped findAll / findAllWithFlashcards
    verify(unitRepo, never()).findAllWithFlashcards();
    verify(unitRepo, never()).findAll();
    verify(subjectRepo, never()).findAll();
  }

  @Test
  void getUnitSummaries_throwsOnNullCommand() {
    assertThrows(IllegalArgumentException.class,
        () -> service.getUnitSummaries(null));
  }

  @Test
  void getUnitSummaries_throwsOnNullUserId() {
    assertThrows(IllegalArgumentException.class,
        () -> service.getUnitSummaries(new FlashcardStudyUseCase.UnitSummaryCommand(null, now)));
  }
}
