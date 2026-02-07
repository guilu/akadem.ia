package com.akdemya.domain.model;

import java.util.UUID;

public class Question {
  private final UUID id;
  private final UUID unitId;
  private final String text;
  private final String explanation;
  private final Difficulty difficulty;

  public enum Difficulty {
    EASY, MEDIUM, HARD
  }

  public Question(UUID id, UUID unitId, String text, String explanation, Difficulty difficulty) {
    this.id = id;
    this.unitId = unitId;
    this.text = text;
    this.explanation = explanation;
    this.difficulty = difficulty;
  }

  public static Question create(UUID unitId, String text, String explanation, Difficulty difficulty) {
    return new Question(UUID.randomUUID(), unitId, text, explanation, difficulty);
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
}
