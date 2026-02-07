package com.akdemya.domain.model;

import java.util.UUID;

public class Unit {
  private final UUID id;
  private final UUID subjectId;
  private final String name;
  private final int orderIndex;

  public Unit(UUID id, UUID subjectId, String name, int orderIndex) {
    this.id = id;
    this.subjectId = subjectId;
    this.name = name;
    this.orderIndex = orderIndex;
  }

  public static Unit create(UUID subjectId, String name, int orderIndex) {
    return new Unit(UUID.randomUUID(), subjectId, name, orderIndex);
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

  public int getOrderIndex() {
    return orderIndex;
  }
}
