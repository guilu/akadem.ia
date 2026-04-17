package com.akdemya.adapter.outbound.persistence.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SyllabusEntityTest {

  @Test
  void defaultConstructor_assignsRandomId() {
    SyllabusEntity entity = new SyllabusEntity();
    assertNotNull(entity.getId());
  }

  @Test
  void defaultVisibility_isGlobal() {
    SyllabusEntity entity = new SyllabusEntity();
    assertEquals("GLOBAL", entity.getVisibility());
  }

  @Test
  void defaultSubjects_isEmptyList() {
    SyllabusEntity entity = new SyllabusEntity();
    assertNotNull(entity.getSubjects());
    assertTrue(entity.getSubjects().isEmpty());
  }

  @Test
  void parameterizedConstructor_setsNameAndDescription() {
    SyllabusEntity entity = new SyllabusEntity("Math", "Math description");
    assertEquals("Math", entity.getName());
    assertEquals("Math description", entity.getDescription());
  }

  @Test
  void setters_updateAllFields() {
    SyllabusEntity entity = new SyllabusEntity();
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    List<SubjectEntity> subjects = new ArrayList<>();

    entity.setId(id);
    entity.setName("Physics");
    entity.setDescription("Physics description");
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);
    entity.setSubjects(subjects);

    assertEquals(id, entity.getId());
    assertEquals("Physics", entity.getName());
    assertEquals("Physics description", entity.getDescription());
    assertEquals("PRIVATE", entity.getVisibility());
    assertEquals(ownerId, entity.getOwnerId());
    assertSame(subjects, entity.getSubjects());
  }
}
