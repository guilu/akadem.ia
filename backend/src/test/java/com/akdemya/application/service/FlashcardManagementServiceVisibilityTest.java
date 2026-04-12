package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
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
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final FlashcardManagementService service = new FlashcardManagementService(flashcardRepo, unitRepo);

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
    Unit globalUnit = Unit.createGlobal(UUID.randomUUID(), "Unit", "desc", 1);
    // Use a global unit so PRIVATE flashcard is allowed
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(
        new Unit(unitId, UUID.randomUUID(), "Unit", "desc", 1, Visibility.GLOBAL, null)));
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
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(
        new Unit(unitId, UUID.randomUUID(), "Unit", "desc", 1, Visibility.GLOBAL, null)));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard created = service.createFlashcardWithVisibility(
        new FlashcardManagementUseCase.CreateCommandWithVisibility(
            unitId, "front", "back", Visibility.GLOBAL, null));

    assertEquals(Visibility.GLOBAL, created.getVisibility());
    assertNull(created.getOwnerId());
  }

  @Test
  void createGlobalFlashcardUnderPrivateUnitThrows() {
    UUID unitId = UUID.randomUUID();
    UUID unitOwnerId = UUID.randomUUID();
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(
        new Unit(unitId, UUID.randomUUID(), "Unit", "desc", 1, Visibility.PRIVATE, unitOwnerId)));

    assertThrows(IllegalArgumentException.class, () ->
        service.createFlashcardWithVisibility(
            new FlashcardManagementUseCase.CreateCommandWithVisibility(
                unitId, "front", "back", Visibility.GLOBAL, null)));
  }

  @Test
  void createPrivateFlashcardInPrivateUnitWithDifferentOwnerThrows() {
    UUID unitId = UUID.randomUUID();
    UUID unitOwnerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(
        new Unit(unitId, UUID.randomUUID(), "Unit", "desc", 1, Visibility.PRIVATE, unitOwnerId)));

    assertThrows(IllegalArgumentException.class, () ->
        service.createFlashcardWithVisibility(
            new FlashcardManagementUseCase.CreateCommandWithVisibility(
                unitId, "front", "back", Visibility.PRIVATE, otherUserId)));
  }

  @Test
  void createPrivateFlashcardInPrivateUnitWithSameOwnerSucceeds() {
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    when(unitRepo.findById(unitId)).thenReturn(Optional.of(
        new Unit(unitId, UUID.randomUUID(), "Unit", "desc", 1, Visibility.PRIVATE, ownerId)));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard created = service.createFlashcardWithVisibility(
        new FlashcardManagementUseCase.CreateCommandWithVisibility(
            unitId, "front", "back", Visibility.PRIVATE, ownerId));

    assertEquals(Visibility.PRIVATE, created.getVisibility());
    assertEquals(ownerId, created.getOwnerId());
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

  // === createFlashcard ===

  @Test
  void createFlashcardDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard result = service.createFlashcard(
        new FlashcardManagementUseCase.CreateCommand(unitId, "front", "back"));

    assertEquals(unitId, result.getUnitId());
    assertEquals("front", result.getFront());
    assertEquals("back", result.getBack());
    verify(flashcardRepo).save(any());
  }

  // === deleteFlashcard ===

  @Test
  void deleteFlashcardDelegatesToRepo() {
    UUID id = UUID.randomUUID();
    service.deleteFlashcard(id);
    verify(flashcardRepo).deleteById(id);
  }

  // === listByUnit ===

  @Test
  void listByUnitDelegatesToRepo() {
    UUID unitId = UUID.randomUUID();
    when(flashcardRepo.findByUnitId(unitId)).thenReturn(List.of());

    service.listByUnit(unitId);

    verify(flashcardRepo).findByUnitId(unitId);
  }

  // === updateFlashcardIfAuthorized ===

  @Test
  void updateFlashcardIfAuthorized_adminCanUpdateGlobalFlashcard() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "old", "old",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UUID adminId = UUID.randomUUID();
    Flashcard updated = service.updateFlashcardIfAuthorized(
        new FlashcardManagementUseCase.UpdateCommand(id, null, "new front", "new back"),
        adminId, true);

    assertEquals("new front", updated.getFront());
    assertEquals(Visibility.GLOBAL, updated.getVisibility());
  }

  @Test
  void updateFlashcardIfAuthorized_nonAdminCannotUpdateGlobal() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "old", "old",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    UUID nonAdmin = UUID.randomUUID();
    assertThrows(AccessDeniedException.class, () ->
        service.updateFlashcardIfAuthorized(
            new FlashcardManagementUseCase.UpdateCommand(id, null, "new", "new"),
            nonAdmin, false));
  }

  @Test
  void updateFlashcardIfAuthorized_ownerCanUpdateOwnPrivateFlashcard() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "old", "old",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, ownerId);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));
    when(flashcardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Flashcard updated = service.updateFlashcardIfAuthorized(
        new FlashcardManagementUseCase.UpdateCommand(id, null, "new front", "new back"),
        ownerId, false);

    assertEquals("new front", updated.getFront());
  }

  @Test
  void updateFlashcardIfAuthorized_nonOwnerCannotUpdatePrivate() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "old", "old",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, ownerId);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    UUID otherId = UUID.randomUUID();
    assertThrows(AccessDeniedException.class, () ->
        service.updateFlashcardIfAuthorized(
            new FlashcardManagementUseCase.UpdateCommand(id, null, "new", "new"),
            otherId, false));
  }

  @Test
  void updateFlashcardIfAuthorized_throwsWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(flashcardRepo.findById(id)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () ->
        service.updateFlashcardIfAuthorized(
            new FlashcardManagementUseCase.UpdateCommand(id, null, "new", "new"),
            UUID.randomUUID(), false));
  }

  // === deleteFlashcardIfAuthorized ===

  @Test
  void deleteFlashcardIfAuthorized_adminCanDeleteGlobal() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    service.deleteFlashcardIfAuthorized(id, UUID.randomUUID(), true);

    verify(flashcardRepo).deleteById(id);
  }

  @Test
  void deleteFlashcardIfAuthorized_nonAdminCannotDeleteGlobal() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.GLOBAL, null);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    assertThrows(AccessDeniedException.class, () ->
        service.deleteFlashcardIfAuthorized(id, UUID.randomUUID(), false));
  }

  @Test
  void deleteFlashcardIfAuthorized_ownerCanDeleteOwnPrivate() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, ownerId);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    service.deleteFlashcardIfAuthorized(id, ownerId, false);

    verify(flashcardRepo).deleteById(id);
  }

  @Test
  void deleteFlashcardIfAuthorized_nonOwnerCannotDeletePrivate() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Flashcard existing = new Flashcard(id, unitId, "front", "back",
        LocalDateTime.now(), LocalDateTime.now(), Visibility.PRIVATE, ownerId);
    when(flashcardRepo.findById(id)).thenReturn(Optional.of(existing));

    assertThrows(AccessDeniedException.class, () ->
        service.deleteFlashcardIfAuthorized(id, UUID.randomUUID(), false));
  }

  @Test
  void deleteFlashcardIfAuthorized_throwsWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(flashcardRepo.findById(id)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () ->
        service.deleteFlashcardIfAuthorized(id, UUID.randomUUID(), false));
  }
}
