package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FlashcardManagementService implements FlashcardManagementUseCase {

  private final FlashcardRepository flashcardRepo;

  public FlashcardManagementService(FlashcardRepository flashcardRepo) {
    this.flashcardRepo = flashcardRepo;
  }

  @Override
  @Transactional
  public Flashcard createFlashcard(CreateCommand command) {
    Flashcard flashcard = Flashcard.create(command.unitId(), command.front(), command.back());
    return flashcardRepo.save(flashcard);
  }

  @Override
  @Transactional
  public Flashcard createFlashcardWithVisibility(CreateCommandWithVisibility command) {
    Flashcard flashcard;
    if (command.visibility() == com.akdemya.domain.model.Visibility.GLOBAL) {
      flashcard = Flashcard.createGlobal(command.unitId(), command.front(), command.back());
    } else {
      flashcard = Flashcard.createPrivate(command.unitId(), command.front(), command.back(), command.ownerId());
    }
    return flashcardRepo.save(flashcard);
  }

  @Override
  @Transactional
  public Flashcard updateFlashcard(UpdateCommand command) {
    Flashcard existing = flashcardRepo.findById(command.id())
        .orElseThrow(() -> new NoSuchElementException("Flashcard not found"));
    UUID unitId = command.unitId() != null ? command.unitId() : existing.getUnitId();
    String front = command.front() != null ? command.front() : existing.getFront();
    String back = command.back() != null ? command.back() : existing.getBack();
    // Preserve visibility and ownerId from existing flashcard to avoid dropping metadata on update
    Flashcard updated = new Flashcard(existing.getId(), unitId, front, back,
        existing.getCreatedAt(), LocalDateTime.now(),
        existing.getVisibility(), existing.getOwnerId());
    return flashcardRepo.save(updated);
  }

  @Override
  @Transactional
  public Flashcard updateFlashcardIfAuthorized(UpdateCommand command, UUID callerId, boolean isAdmin) {
    Flashcard existing = flashcardRepo.findById(command.id())
        .orElseThrow(() -> new NoSuchElementException("Flashcard not found"));
    if (existing.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can update GLOBAL flashcards");
    }
    if (existing.getVisibility() == Visibility.PRIVATE
        && !existing.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot update another user's PRIVATE flashcard");
    }
    UUID unitId = command.unitId() != null ? command.unitId() : existing.getUnitId();
    String front = command.front() != null ? command.front() : existing.getFront();
    String back = command.back() != null ? command.back() : existing.getBack();
    Flashcard updated = new Flashcard(existing.getId(), unitId, front, back,
        existing.getCreatedAt(), LocalDateTime.now(),
        existing.getVisibility(), existing.getOwnerId());
    return flashcardRepo.save(updated);
  }

  @Override
  @Transactional
  public void deleteFlashcard(UUID id) {
    flashcardRepo.deleteById(id);
  }

  @Override
  @Transactional
  public void deleteFlashcardIfAuthorized(UUID id, UUID callerId, boolean isAdmin) {
    Flashcard existing = flashcardRepo.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Flashcard not found: " + id));
    if (existing.getVisibility() == Visibility.GLOBAL && !isAdmin) {
      throw new AccessDeniedException("Only admins can delete GLOBAL flashcards");
    }
    if (existing.getVisibility() == Visibility.PRIVATE
        && !existing.getOwnerId().equals(callerId)) {
      throw new AccessDeniedException("Cannot delete another user's PRIVATE flashcard");
    }
    flashcardRepo.deleteById(id);
  }

  @Override
  public List<Flashcard> listByUnit(UUID unitId) {
    return flashcardRepo.findByUnitId(unitId);
  }

  @Override
  public List<Flashcard> listVisibleByUnit(UUID unitId, UUID userId) {
    return flashcardRepo.findVisibleByUnitIdAndUserId(unitId, userId);
  }
}
