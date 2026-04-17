package com.akdemya.domain.model;

import java.util.UUID;

public class Subject {
  private final UUID id;
  private final String name;
  private final String description;
  private final Visibility visibility;
  private final UUID ownerId;
  private final UUID syllabusId;

  public Subject(UUID id, String name, String description, Visibility visibility, UUID ownerId, UUID syllabusId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.visibility = visibility;
    this.ownerId = ownerId;
    this.syllabusId = syllabusId;
  }

  /** Backward-compatible constructor: defaults to GLOBAL, no owner, no syllabus. */
  public Subject(UUID id, String name, String description, Visibility visibility, UUID ownerId) {
    this(id, name, description, visibility, ownerId, null);
  }

  /** Backward-compatible constructor: defaults to GLOBAL, no owner. */
  public Subject(UUID id, String name, String description) {
    this(id, name, description, Visibility.GLOBAL, null, null);
  }

  public static Subject create(String name, String description) {
    return new Subject(UUID.randomUUID(), name, description, Visibility.GLOBAL, null, null);
  }

  public static Subject createGlobal(String name, String description) {
    return new Subject(UUID.randomUUID(), name, description, Visibility.GLOBAL, null, null);
  }

  public static Subject createGlobal(String name, String description, UUID syllabusId) {
    return new Subject(UUID.randomUUID(), name, description, Visibility.GLOBAL, null, syllabusId);
  }

  public static Subject createPrivate(String name, String description, UUID ownerId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE subject");
    return new Subject(UUID.randomUUID(), name, description, Visibility.PRIVATE, ownerId, null);
  }

  public static Subject createPrivate(String name, String description, UUID ownerId, UUID syllabusId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE subject");
    return new Subject(UUID.randomUUID(), name, description, Visibility.PRIVATE, ownerId, syllabusId);
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

  public UUID getSyllabusId() {
    return syllabusId;
  }
}
