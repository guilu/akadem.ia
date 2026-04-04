package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests that flashcard update preserves visibility and ownerId metadata,
 * and that createFlashcardWithVisibility properly sets the visibility.
 */
class FlashcardManagementServiceVisibilityTest {

  private final FlashcardRepository flashcardRepo = mock(FlashcardRepository.class);
  private final FlashcardManagementService service = new FlashcardManagementService(flashcardRepo);

  // === Update preserves visibility ===

  @Test
  void updateFlashcardPreservesPrivateVisibilityAndOwnerId() {
    UUID flashcardId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

    Flashcard existing = new Flashcard(flashcardId, unitId, "old front", "old back",
        createdAt, createdAt, Visibility.PRIVATE, ownerId);
    when(flashcardRepo.findById(flashcardId)).thenReturn(Optional.of(existing));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard updated = service.updateFlashcard(
        new FlashcardManagementUseCase.UpdateCommand(flashcardId, null, "new front", "new back"));

    assertEquals(Visibility.PRIVATE, updated.getVisibility());
    assertEquals(ownerId, updated.getOwnerId());
    assertEquals("new front", updated.getFront());
    assertEquals("new back", updated.getBack());
    assertEquals(createdAt, updated.getCreatedAt());
  }

  @Test
  void updateFlashcardPreservesGlobalVisibilityWithNullOwner() {
    UUID flashcardId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

    Flashcard existing = new Flashcard(flashcardId, unitId, "old front", "old back",
        createdAt, createdAt, Visibility.GLOBAL, null);
    when(flashcardRepo.findById(flashcardId)).thenReturn(Optional.of(existing));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard updated = service.updateFlashcard(
        new FlashcardManagementUseCase.UpdateCommand(flashcardId, null, "new front", "new back"));

    assertEquals(Visibility.GLOBAL, updated.getVisibility());
    assertNull(updated.getOwnerId());
  }

  @Test
  void updateFlashcardPreservesUnitIdWhenNotProvided() {
    UUID flashcardId = UUID.randomUUID();
    UUID originalUnitId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

    Flashcard existing = new Flashcard(flashcardId, originalUnitId, "front", "back",
        createdAt, createdAt, Visibility.PRIVATE, UUID.randomUUID());
    when(flashcardRepo.findById(flashcardId)).thenReturn(Optional.of(existing));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard updated = service.updateFlashcard(
        new FlashcardManagementUseCase.UpdateCommand(flashcardId, null, "new front", null));

    assertEquals(originalUnitId, updated.getUnitId());
    assertEquals("new front", updated.getFront());
    assertEquals("back", updated.getBack()); // unchanged
  }

  // === createFlashcardWithVisibility ===

  @Test
  void createFlashcardWithPrivateVisibilityCreatesPrivateFlashcard() {
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard created = service.createFlashcardWithVisibility(
        new FlashcardManagementUseCase.CreateCommandWithVisibility(
            unitId, "front", "back", Visibility.PRIVATE, ownerId));

    assertEquals(Visibility.PRIVATE, created.getVisibility());
    assertEquals(ownerId, created.getOwnerId());
    assertEquals(unitId, created.getUnitId());
  }

  @Test
  void createFlashcardWithGlobalVisibilityCreatesGlobalFlashcard() {
    UUID unitId = UUID.randomUUID();
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard created = service.createFlashcardWithVisibility(
        new FlashcardManagementUseCase.CreateCommandWithVisibility(
            unitId, "front", "back", Visibility.GLOBAL, null));

    assertEquals(Visibility.GLOBAL, created.getVisibility());
    assertNull(created.getOwnerId());
  }

  // === listVisibleByUnit ===

  @Test
  void listVisibleByUnitDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Flashcard card = new Flashcard(UUID.randomUUID(), unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, userId);
    when(flashcardRepo.findVisibleByUnitIdAndUserId(unitId, userId)).thenReturn(List.of(card));

    List<Flashcard> result = service.listVisibleByUnit(unitId, userId);

    assertEquals(1, result.size());
    verify(flashcardRepo).findVisibleByUnitIdAndUserId(unitId, userId);
  }
}
