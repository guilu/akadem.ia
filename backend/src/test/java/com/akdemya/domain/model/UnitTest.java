package com.akdemya.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnitTest {

  @Test
  void createGlobalUnitHasGlobalVisibilityAndNullOwner() {
    UUID subjectId = UUID.randomUUID();
    Unit u = Unit.createGlobal(subjectId, "Algebra", "desc", 1);
    assertEquals(Visibility.GLOBAL, u.getVisibility());
    assertNull(u.getOwnerId());
    assertNotNull(u.getId());
    assertEquals(subjectId, u.getSubjectId());
    assertEquals("Algebra", u.getName());
    assertEquals("desc", u.getDescription());
    assertEquals(1, u.getOrderIndex());
  }

  @Test
  void createPrivateUnitHasPrivateVisibilityAndOwner() {
    UUID subjectId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Unit u = Unit.createPrivate(subjectId, "My Unit", "desc", 2, ownerId);
    assertEquals(Visibility.PRIVATE, u.getVisibility());
    assertEquals(ownerId, u.getOwnerId());
    assertNotNull(u.getId());
  }

  @Test
  void createPrivateUnitWithNullOwnerIdThrows() {
    UUID subjectId = UUID.randomUUID();
    assertThrows(IllegalArgumentException.class,
        () -> Unit.createPrivate(subjectId, "My Unit", "desc", 1, null));
  }

  @Test
  void createFactoryDefaultsToGlobal() {
    UUID subjectId = UUID.randomUUID();
    Unit u = Unit.create(subjectId, "Unit A", "desc", 0);
    assertEquals(Visibility.GLOBAL, u.getVisibility());
    assertNull(u.getOwnerId());
  }

  @Test
  void backwardCompatibleConstructorDefaultsToGlobal() {
    UUID id = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Unit u = new Unit(id, subjectId, "Unit B", "desc", 3);
    assertEquals(Visibility.GLOBAL, u.getVisibility());
    assertNull(u.getOwnerId());
    assertEquals(id, u.getId());
    assertEquals(subjectId, u.getSubjectId());
    assertEquals("Unit B", u.getName());
    assertEquals("desc", u.getDescription());
    assertEquals(3, u.getOrderIndex());
  }

  @Test
  void fullConstructorSetsAllFields() {
    UUID id = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Unit u = new Unit(id, subjectId, "Unit C", "detail", 5, Visibility.PRIVATE, ownerId);
    assertEquals(id, u.getId());
    assertEquals(subjectId, u.getSubjectId());
    assertEquals("Unit C", u.getName());
    assertEquals("detail", u.getDescription());
    assertEquals(5, u.getOrderIndex());
    assertEquals(Visibility.PRIVATE, u.getVisibility());
    assertEquals(ownerId, u.getOwnerId());
  }
}
