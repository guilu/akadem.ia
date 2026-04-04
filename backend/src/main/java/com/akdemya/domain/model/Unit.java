package com.akdemya.domain.model;

import java.util.UUID;

public class Unit {
  private final UUID id;
  private final UUID subjectId;
  private final String name;
  private final String description;
  private final int orderIndex;
  private final Visibility visibility;
  private final UUID ownerId;

  public Unit(UUID id, UUID subjectId, String name, String description, int orderIndex,
              Visibility visibility, UUID ownerId) {
    this.id = id;
    this.subjectId = subjectId;
    this.name = name;
    this.description = description;
    this.orderIndex = orderIndex;
    this.visibility = visibility;
    this.ownerId = ownerId;
  }

  /** Backward-compatible constructor: defaults to GLOBAL, no owner. */
  public Unit(UUID id, UUID subjectId, String name, String description, int orderIndex) {
    this(id, subjectId, name, description, orderIndex, Visibility.GLOBAL, null);
  }

  public static Unit create(UUID subjectId, String name, String description, int orderIndex) {
    return new Unit(UUID.randomUUID(), subjectId, name, description, orderIndex, Visibility.GLOBAL, null);
  }

  public static Unit createGlobal(UUID subjectId, String name, String description, int orderIndex) {
    return new Unit(UUID.randomUUID(), subjectId, name, description, orderIndex, Visibility.GLOBAL, null);
  }

  public static Unit createPrivate(UUID subjectId, String name, String description, int orderIndex, UUID ownerId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE unit");
    return new Unit(UUID.randomUUID(), subjectId, name, description, orderIndex, Visibility.PRIVATE, ownerId);
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

  public Visibility getVisibility() {
    return visibility;
  }

  public UUID getOwnerId() {
    return ownerId;
  }
}
