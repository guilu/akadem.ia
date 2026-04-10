package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.UserSettingsEntity;
import com.akdemya.domain.model.UserSettings;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSettingsMapperTest {

  private final UserSettingsMapper mapper = new UserSettingsMapper();

  @Test
  void toDomain_mapsAllFields() {
    UUID userId = UUID.randomUUID();
    UserSettingsEntity entity = new UserSettingsEntity();
    entity.setUserId(userId);
    entity.setNewCardsLimit(20);
    entity.setReviewCardsLimit(100);
    entity.setPenaltyRatio(4);

    UserSettings domain = mapper.toDomain(entity);

    assertEquals(userId, domain.userId());
    assertEquals(20, domain.newCardsLimit());
    assertEquals(100, domain.reviewCardsLimit());
    assertEquals(4, domain.penaltyRatio());
  }

  @Test
  void toEntity_withNullExisting_createsNewEntity() {
    UUID userId = UUID.randomUUID();
    UserSettings domain = new UserSettings(userId, 15, 80, 3);

    UserSettingsEntity entity = mapper.toEntity(domain, null);

    assertNotNull(entity);
    assertEquals(userId, entity.getUserId());
    assertEquals(15, entity.getNewCardsLimit());
    assertEquals(80, entity.getReviewCardsLimit());
    assertEquals(3, entity.getPenaltyRatio());
    assertNotNull(entity.getCreatedAt());
    assertNotNull(entity.getUpdatedAt());
  }

  @Test
  void toEntity_withExisting_updatesExistingEntity() {
    UUID userId = UUID.randomUUID();
    UserSettingsEntity existing = new UserSettingsEntity();
    existing.setUserId(userId);
    existing.setNewCardsLimit(5);
    existing.setReviewCardsLimit(30);
    existing.setPenaltyRatio(2);

    UserSettings domain = new UserSettings(userId, 25, 150, 5);
    UserSettingsEntity result = mapper.toEntity(domain, existing);

    assertSame(existing, result);
    assertEquals(userId, result.getUserId());
    assertEquals(25, result.getNewCardsLimit());
    assertEquals(150, result.getReviewCardsLimit());
    assertEquals(5, result.getPenaltyRatio());
  }

  @Test
  void toEntity_withNullExisting_setsCreatedAt() {
    UUID userId = UUID.randomUUID();
    UserSettings domain = new UserSettings(userId, 10, 50, 3);

    UserSettingsEntity entity = mapper.toEntity(domain, null);

    assertNotNull(entity.getCreatedAt());
  }

  @Test
  void toEntity_withExisting_updatesUpdatedAt() {
    UUID userId = UUID.randomUUID();
    UserSettingsEntity existing = new UserSettingsEntity();
    existing.setUserId(userId);
    UserSettings domain = new UserSettings(userId, 10, 50, 3);

    UserSettingsEntity result = mapper.toEntity(domain, existing);

    assertNotNull(result.getUpdatedAt());
  }
}
