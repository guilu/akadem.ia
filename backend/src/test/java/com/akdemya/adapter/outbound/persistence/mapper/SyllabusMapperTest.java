package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SyllabusEntity;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyllabusMapperTest {

  private final SyllabusMapper mapper = new SyllabusMapper();

  @Test
  void toDomainMapsAllFieldsWithExplicitVisibility() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    SyllabusEntity entity = new SyllabusEntity("Math Syllabus", "description");
    entity.setId(id);
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);

    Syllabus domain = mapper.toDomain(entity);

    assertEquals(id, domain.getId());
    assertEquals("Math Syllabus", domain.getName());
    assertEquals("description", domain.getDescription());
    assertEquals(Visibility.PRIVATE, domain.getVisibility());
    assertEquals(ownerId, domain.getOwnerId());
  }

  @Test
  void toDomainDefaultsVisibilityToGlobalWhenNull() {
    SyllabusEntity entity = new SyllabusEntity("Physics", "desc");
    entity.setId(UUID.randomUUID());
    entity.setVisibility(null);

    Syllabus domain = mapper.toDomain(entity);

    assertEquals(Visibility.GLOBAL, domain.getVisibility());
  }

  @Test
  void toDomainMapsGlobalVisibility() {
    SyllabusEntity entity = new SyllabusEntity("Chemistry", "desc");
    entity.setId(UUID.randomUUID());
    entity.setVisibility("GLOBAL");

    Syllabus domain = mapper.toDomain(entity);

    assertEquals(Visibility.GLOBAL, domain.getVisibility());
    assertNull(domain.getOwnerId());
  }

  @Test
  void toEntityMapsAllFieldsWithPrivateVisibility() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Syllabus domain = new Syllabus(id, "My Syllabus", "detail", Visibility.PRIVATE, ownerId);

    SyllabusEntity entity = mapper.toEntity(domain);

    assertEquals(id, entity.getId());
    assertEquals("My Syllabus", entity.getName());
    assertEquals("detail", entity.getDescription());
    assertEquals("PRIVATE", entity.getVisibility());
    assertEquals(ownerId, entity.getOwnerId());
  }

  @Test
  void toEntityMapsGlobalVisibility() {
    UUID id = UUID.randomUUID();
    Syllabus domain = new Syllabus(id, "Global Syllabus", "desc", Visibility.GLOBAL, null);

    SyllabusEntity entity = mapper.toEntity(domain);

    assertEquals("GLOBAL", entity.getVisibility());
    assertNull(entity.getOwnerId());
  }

  @Test
  void toEntityDefaultsVisibilityToGlobalWhenNull() {
    UUID id = UUID.randomUUID();
    Syllabus domain = new Syllabus(id, "Null Vis Syllabus", "desc", null, null);

    SyllabusEntity entity = mapper.toEntity(domain);

    assertEquals("GLOBAL", entity.getVisibility());
  }
}
