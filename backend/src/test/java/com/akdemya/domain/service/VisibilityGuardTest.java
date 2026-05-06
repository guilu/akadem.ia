package com.akdemya.domain.service;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VisibilityGuardTest {

  private final VisibilityGuard guard = new VisibilityGuard();

  @Test
  void globalSubjectIsAccessibleByAnyUser() {
    Subject subject = Subject.createGlobal("Math", "desc");
    UUID randomUser = UUID.randomUUID();
    assertTrue(guard.canAccess(subject.getVisibility(), subject.getOwnerId(), randomUser));
  }

  @Test
  void globalSubjectIsAccessibleByNullUser() {
    Subject subject = Subject.createGlobal("Math", "desc");
    assertTrue(guard.canAccess(subject.getVisibility(), subject.getOwnerId(), null));
  }

  @Test
  void privateSubjectIsAccessibleByOwner() {
    UUID ownerId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Math", "desc", ownerId);
    assertTrue(guard.canAccess(subject.getVisibility(), subject.getOwnerId(), ownerId));
  }

  @Test
  void privateSubjectIsNotAccessibleByDifferentUser() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Math", "desc", ownerId);
    assertFalse(guard.canAccess(subject.getVisibility(), subject.getOwnerId(), otherId));
  }

  @Test
  void privateSubjectIsNotAccessibleByNullUser() {
    UUID ownerId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Math", "desc", ownerId);
    assertFalse(guard.canAccess(subject.getVisibility(), subject.getOwnerId(), null));
  }

  @Test
  void checkAccessThrowsForPrivateSubjectAccessedByWrongUser() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Math", "desc", ownerId);
    assertThrows(SecurityException.class,
        () -> guard.checkAccess(subject.getVisibility(), subject.getOwnerId(), otherId));
  }

  @Test
  void checkAccessDoesNotThrowForGlobalSubject() {
    Subject subject = Subject.createGlobal("Math", "desc");
    assertDoesNotThrow(() -> guard.checkAccess(subject.getVisibility(), subject.getOwnerId(), UUID.randomUUID()));
  }

  @Test
  void checkAccessDoesNotThrowForPrivateSubjectAccessedByOwner() {
    UUID ownerId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Math", "desc", ownerId);
    assertDoesNotThrow(() -> guard.checkAccess(subject.getVisibility(), subject.getOwnerId(), ownerId));
  }
}
