package com.akdemya.domain.model;

import java.util.UUID;

public class Question {
  private final UUID id;
  private final UUID unitId;
  private final String text;
  private final String explanation;
  private final Difficulty difficulty;
  private final Visibility visibility;
  private final UUID ownerId;

  public enum Difficulty {
    EASY, MEDIUM, HARD
  }

  public Question(UUID id, UUID unitId, String text, String explanation, Difficulty difficulty,
                  Visibility visibility, UUID ownerId) {
    this.id = id;
    this.unitId = unitId;
    this.text = text;
    this.explanation = explanation;
    this.difficulty = difficulty;
    this.visibility = visibility;
    this.ownerId = ownerId;
  }

  /** Backward-compatible constructor: defaults to GLOBAL, no owner. */
  public Question(UUID id, UUID unitId, String text, String explanation, Difficulty difficulty) {
    this(id, unitId, text, explanation, difficulty, Visibility.GLOBAL, null);
  }

  public static Question create(UUID unitId, String text, String explanation, Difficulty difficulty) {
    return new Question(UUID.randomUUID(), unitId, text, explanation, difficulty, Visibility.GLOBAL, null);
  }

  public static Question createGlobal(UUID unitId, String text, String explanation, Difficulty difficulty) {
    return new Question(UUID.randomUUID(), unitId, text, explanation, difficulty, Visibility.GLOBAL, null);
  }

  public static Question createPrivate(UUID unitId, String text, String explanation, Difficulty difficulty,
                                       UUID ownerId) {
    if (ownerId == null) throw new IllegalArgumentException("ownerId cannot be null for PRIVATE question");
    return new Question(UUID.randomUUID(), unitId, text, explanation, difficulty, Visibility.PRIVATE, ownerId);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUnitId() {
    return unitId;
  }

  public String getText() {
    return text;
  }

  public String getExplanation() {
    return explanation;
  }

  public Difficulty getDifficulty() {
    return difficulty;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public UUID getOwnerId() {
    return ownerId;
  }
}
