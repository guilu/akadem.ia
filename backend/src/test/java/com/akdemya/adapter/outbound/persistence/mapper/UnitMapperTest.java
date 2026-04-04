package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnitMapperTest {

  private final UnitMapper mapper = new UnitMapper();

  @Test
  void toDomainMapsAllFieldsWithExplicitVisibility() {
    UnitEntity entity = new UnitEntity(null, "Algebra", "desc", 1);
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    entity.setId(id);
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);

    Unit domain = mapper.toDomain(entity);

    assertEquals(id, domain.getId());
    assertEquals("Algebra", domain.getName());
    assertEquals("desc", domain.getDescription());
    assertEquals(1, domain.getOrderIndex());
    assertEquals(Visibility.PRIVATE, domain.getVisibility());
    assertEquals(ownerId, domain.getOwnerId());
    assertNull(domain.getSubjectId()); // no subject entity set
  }

  @Test
  void toDomainDefaultsVisibilityToGlobalWhenNull() {
    UnitEntity entity = new UnitEntity(null, "Algebra", "desc", 1);
    entity.setVisibility(null);

    Unit domain = mapper.toDomain(entity);

    assertEquals(Visibility.GLOBAL, domain.getVisibility());
  }

  @Test
  void toEntityMapsAllFieldsWithExplicitVisibility() {
    UUID id = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Unit domain = new Unit(id, subjectId, "Calculus", "detail", 2, Visibility.PRIVATE, ownerId);

    UnitEntity entity = mapper.toEntity(domain);

    assertEquals(id, entity.getId());
    assertEquals("Calculus", entity.getName());
    assertEquals("detail", entity.getDescription());
    assertEquals(2, entity.getOrderIndex());
    assertEquals("PRIVATE", entity.getVisibility());
    assertEquals(ownerId, entity.getOwnerId());
  }

  @Test
  void toEntityDefaultsVisibilityToGlobalWhenDomainVisibilityIsNull() {
    UUID id = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Unit domain = new Unit(id, subjectId, "Stats", "desc", 0, null, null);

    UnitEntity entity = mapper.toEntity(domain);

    assertEquals("GLOBAL", entity.getVisibility());
    assertNull(entity.getOwnerId());
  }
}
