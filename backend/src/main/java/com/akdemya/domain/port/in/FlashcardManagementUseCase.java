package com.akdemya.domain.port.in;

import com.akdemya.domain.model.Flashcard;

import java.util.List;
import java.util.UUID;

public interface FlashcardManagementUseCase {

  Flashcard createFlashcard(CreateCommand command);

  Flashcard updateFlashcard(UpdateCommand command);

  void deleteFlashcard(UUID id);

  List<Flashcard> listByUnit(UUID unitId);

  record CreateCommand(UUID unitId, String front, String back) {}

  record UpdateCommand(UUID id, UUID unitId, String front, String back) {}
}
