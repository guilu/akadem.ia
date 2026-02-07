package com.akdemya.domain.model;

import java.util.UUID;

public class Answer {
  private final UUID id;
  private final UUID questionId;
  private final String text;
  private final boolean correct;

  public Answer(UUID id, UUID questionId, String text, boolean correct) {
    this.id = id;
    this.questionId = questionId;
    this.text = text;
    this.correct = correct;
  }

  public static Answer create(UUID questionId, String text, boolean correct) {
    return new Answer(UUID.randomUUID(), questionId, text, correct);
  }

  public UUID getId() {
    return id;
  }

  public UUID getQuestionId() {
    return questionId;
  }

  public String getText() {
    return text;
  }

  public boolean isCorrect() {
    return correct;
  }
}
