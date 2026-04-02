package com.akdemya.adapter.outbound.persistence.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppUserEntityTest {

  @Test
  void defaultConstructor_assignsRandomId() {
    AppUserEntity entity = new AppUserEntity();

    assertNotNull(entity.getId());
  }

  @Test
  void emailPasswordConstructor_setsEmailAndPasswordHash() {
    AppUserEntity entity = new AppUserEntity("user@example.com", "hashed_password");

    assertEquals("user@example.com", entity.getEmail());
    assertEquals("hashed_password", entity.getPasswordHash());
  }

  @Test
  void defaultRole_isStudent() {
    AppUserEntity entity = new AppUserEntity();

    assertEquals("STUDENT", entity.getRole());
  }

  @Test
  void setters_updateAllFields() {
    AppUserEntity entity = new AppUserEntity();
    UUID id = UUID.randomUUID();

    entity.setId(id);
    entity.setEmail("test@example.com");
    entity.setPasswordHash("new_hash");
    entity.setRole("ADMIN");
    entity.setFirstName("John");
    entity.setLastName("Doe");
    entity.setOccupation("Developer");

    assertEquals(id, entity.getId());
    assertEquals("test@example.com", entity.getEmail());
    assertEquals("new_hash", entity.getPasswordHash());
    assertEquals("ADMIN", entity.getRole());
    assertEquals("John", entity.getFirstName());
    assertEquals("Doe", entity.getLastName());
    assertEquals("Developer", entity.getOccupation());
  }

  @Test
  void optionalFields_areNullByDefault() {
    AppUserEntity entity = new AppUserEntity();

    assertNull(entity.getFirstName());
    assertNull(entity.getLastName());
    assertNull(entity.getOccupation());
    assertNull(entity.getPasswordHash());
  }
}
