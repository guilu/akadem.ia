package com.akdemya.domain.port.in;

import com.akdemya.domain.model.Flashcard;

import java.util.List;
import java.util.UUID;

public interface FlashcardManagementUseCase {

  Flashcard createFlashcard(CreateCommand command);

  Flashcard createFlashcardWithVisibility(CreateCommandWithVisibility command);

  Flashcard updateFlashcard(UpdateCommand command);

  Flashcard updateFlashcardIfAuthorized(UpdateCommand command, UUID callerId, boolean isAdmin);

  void deleteFlashcard(UUID id);

  void deleteFlashcardIfAuthorized(UUID id, UUID callerId, boolean isAdmin);

  List<Flashcard> listByUnit(UUID unitId);

  List<Flashcard> listVisibleByUnit(UUID unitId, UUID userId);

  record CreateCommand(UUID unitId, String front, String back) {}

  record CreateCommandWithVisibility(UUID unitId, String front, String back,
                                     com.akdemya.domain.model.Visibility visibility, UUID ownerId) {}

  record UpdateCommand(UUID id, UUID unitId, String front, String back) {}
}
