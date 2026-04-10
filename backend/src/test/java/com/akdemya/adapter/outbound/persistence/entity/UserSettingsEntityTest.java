package com.akdemya.adapter.outbound.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSettingsEntityTest {

  @Test
  void defaultConstructor_assignsRandomId() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertNotNull(entity.getId());
  }

  @Test
  void defaultPenaltyRatio_isThree() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertEquals(3, entity.getPenaltyRatio());
  }

  @Test
  void defaultCreatedAt_isNotNull() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertNotNull(entity.getCreatedAt());
  }

  @Test
  void defaultUpdatedAt_isNotNull() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertNotNull(entity.getUpdatedAt());
  }

  @Test
  void setters_updateAllFields() {
    UserSettingsEntity entity = new UserSettingsEntity();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    entity.setId(id);
    entity.setUserId(userId);
    entity.setNewCardsLimit(20);
    entity.setReviewCardsLimit(100);
    entity.setPenaltyRatio(5);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    assertEquals(id, entity.getId());
    assertEquals(userId, entity.getUserId());
    assertEquals(20, entity.getNewCardsLimit());
    assertEquals(100, entity.getReviewCardsLimit());
    assertEquals(5, entity.getPenaltyRatio());
    assertEquals(now, entity.getCreatedAt());
    assertEquals(now, entity.getUpdatedAt());
  }

  @Test
  void userId_isNullByDefault() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertNull(entity.getUserId());
  }

  @Test
  void newCardsLimit_defaultsToZero() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertEquals(0, entity.getNewCardsLimit());
  }

  @Test
  void reviewCardsLimit_defaultsToZero() {
    UserSettingsEntity entity = new UserSettingsEntity();
    assertEquals(0, entity.getReviewCardsLimit());
  }
}
