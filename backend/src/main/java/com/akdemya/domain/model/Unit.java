package com.akdemya.domain.model;

import java.util.UUID;

public class Unit {
  private final UUID id;
  private final UUID subjectId;
  private final String name;
  private final String description;
  private final int orderIndex;

  public Unit(UUID id, UUID subjectId, String name, String description, int orderIndex) {
    this.id = id;
    this.subjectId = subjectId;
    this.name = name;
    this.description = description;
    this.orderIndex = orderIndex;
  }

  public static Unit create(UUID subjectId, String name, String description, int orderIndex) {
    return new Unit(UUID.randomUUID(), subjectId, name, description, orderIndex);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSubjectId() {
    return subjectId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getOrderIndex() {
    return orderIndex;
  }
}
