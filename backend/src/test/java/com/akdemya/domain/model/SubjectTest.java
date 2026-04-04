package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubjectTest {

  @Test
  void createGlobalSubjectHasGlobalVisibilityAndNullOwner() {
    Subject s = Subject.createGlobal("Math", "desc");
    assertEquals(Visibility.GLOBAL, s.getVisibility());
    assertNull(s.getOwnerId());
    assertNotNull(s.getId());
  }

  @Test
  void createPrivateSubjectHasPrivateVisibilityAndOwner() {
    UUID ownerId = UUID.randomUUID();
    Subject s = Subject.createPrivate("My Math", "desc", ownerId);
    assertEquals(Visibility.PRIVATE, s.getVisibility());
    assertEquals(ownerId, s.getOwnerId());
  }

  @Test
  void createPrivateWithNullOwnerIdThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> Subject.createPrivate("My Math", "desc", null));
  }
}
