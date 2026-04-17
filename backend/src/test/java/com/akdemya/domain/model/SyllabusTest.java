package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyllabusTest {

  @Test
  void createGlobalSyllabusHasGlobalVisibilityAndNullOwner() {
    Syllabus s = Syllabus.createGlobal("Default", "Temario por defecto");
    assertEquals(Visibility.GLOBAL, s.getVisibility());
    assertNull(s.getOwnerId());
    assertNotNull(s.getId());
    assertEquals("Default", s.getName());
    assertEquals("Temario por defecto", s.getDescription());
  }

  @Test
  void createPrivateSyllabusHasPrivateVisibilityAndOwner() {
    UUID ownerId = UUID.randomUUID();
    Syllabus s = Syllabus.createPrivate("My Syllabus", "My personal syllabus", ownerId);
    assertEquals(Visibility.PRIVATE, s.getVisibility());
    assertEquals(ownerId, s.getOwnerId());
    assertNotNull(s.getId());
  }

  @Test
  void createPrivateWithNullOwnerIdThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> Syllabus.createPrivate("My Syllabus", "desc", null));
  }
}
