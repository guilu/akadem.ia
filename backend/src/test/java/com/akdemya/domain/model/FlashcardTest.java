package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardTest {

  private final UUID unitId = UUID.randomUUID();

  // --- unitId ---

  @Test
  void unitIdCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(null, "front", "back"));
  }

  // --- front ---

  @Test
  void frontCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, null, "back"));
  }

  @Test
  void frontCannotBeEmpty() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, "", "back"));
  }

  @Test
  void frontCannotBeBlankWhitespace() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, "   ", "back"));
  }

  // --- back ---

  @Test
  void backCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, "front", null));
  }

  @Test
  void backCannotBeEmpty() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, "front", ""));
  }

  @Test
  void backCannotBeBlankWhitespace() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.create(unitId, "front", "   "));
  }

  // --- happy path ---

  @Test
  void validFlashcardIsCreated() {
    var f = Flashcard.create(unitId, "What is Java?", "A programming language");
    assertNotNull(f.getId());
    assertEquals(unitId, f.getUnitId());
    assertEquals("What is Java?", f.getFront());
    assertEquals("A programming language", f.getBack());
    assertNotNull(f.getCreatedAt());
    assertNotNull(f.getUpdatedAt());
  }

  @Test
  void frontAndBackAreTrimmed() {
    var f = Flashcard.create(unitId, "  front  ", "  back  ");
    assertEquals("front", f.getFront());
    assertEquals("back", f.getBack());
  }

  // --- visibility / ownerId ---

  @Test
  void createDefaultsToGlobalVisibility() {
    var f = Flashcard.create(unitId, "front", "back");
    assertEquals(Visibility.GLOBAL, f.getVisibility());
    assertNull(f.getOwnerId());
  }

  @Test
  void createGlobalHasGlobalVisibility() {
    var f = Flashcard.createGlobal(unitId, "front", "back");
    assertEquals(Visibility.GLOBAL, f.getVisibility());
    assertNull(f.getOwnerId());
  }

  @Test
  void createPrivateHasPrivateVisibilityAndOwner() {
    UUID ownerId = UUID.randomUUID();
    var f = Flashcard.createPrivate(unitId, "front", "back", ownerId);
    assertEquals(Visibility.PRIVATE, f.getVisibility());
    assertEquals(ownerId, f.getOwnerId());
  }

  @Test
  void createPrivateWithNullOwnerThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> Flashcard.createPrivate(unitId, "front", "back", null));
  }
}
