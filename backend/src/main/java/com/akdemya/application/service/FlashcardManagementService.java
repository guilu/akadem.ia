package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
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
  public Flashcard updateFlashcard(UpdateCommand command) {
    Flashcard existing = flashcardRepo.findById(command.id())
        .orElseThrow(() -> new NoSuchElementException("Flashcard not found"));
    UUID unitId = command.unitId() != null ? command.unitId() : existing.getUnitId();
    String front = command.front() != null ? command.front() : existing.getFront();
    String back = command.back() != null ? command.back() : existing.getBack();
    Flashcard updated = new Flashcard(existing.getId(), unitId, front, back,
        existing.getCreatedAt(), LocalDateTime.now());
    return flashcardRepo.save(updated);
  }

  @Override
  @Transactional
  public void deleteFlashcard(UUID id) {
    flashcardRepo.deleteById(id);
  }

  @Override
  public List<Flashcard> listByUnit(UUID unitId) {
    return flashcardRepo.findByUnitId(unitId);
  }
}
