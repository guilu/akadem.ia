package com.akdemya.domain.model;

import java.util.UUID;

public class Subject {
  private final UUID id;
  private final String name;
  private final String description;
  private final Visibility visibility;
  private final UUID ownerId;

  public Subject(UUID id, String name, String description, Visibility visibility, UUID ownerId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.visibility = visibility;
    this.ownerId = ownerId;
  }

  /** Backward-compatible constructor: defaults to GLOBAL, no owner. */
  public Subject(UUID id, String name, String description) {
    this(id, name, description, Visibility.GLOBAL, null);
  }

  public static Subject create(String name, String description) {
    return new Subject(UUID.randomUUID(), name, description, Visibility.GLOBAL, null);
  }

  public static Subject createGlobal(String name, String description) {
    return new Subject(UUID.randomUUID(), name, description, Visibility.GLOBAL, null);
  }

  public static Subject createPrivate(String name, String description, UUID ownerId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE subject");
    return new Subject(UUID.randomUUID(), name, description, Visibility.PRIVATE, ownerId);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public UUID getOwnerId() {
    return ownerId;
  }
}
