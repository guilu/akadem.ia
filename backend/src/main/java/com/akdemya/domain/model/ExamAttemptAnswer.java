package com.akdemya.domain.model;

import java.util.UUID;

public class ExamAttemptAnswer {
  private final UUID id;
  private final UUID examAttemptId;
  private final UUID questionId;
  private final UUID answerId;

  public ExamAttemptAnswer(UUID id, UUID examAttemptId, UUID questionId, UUID answerId) {
    this.id = id;
    this.examAttemptId = examAttemptId;
    this.questionId = questionId;
    this.answerId = answerId;
  }

  public static ExamAttemptAnswer create(UUID examAttemptId, UUID questionId, UUID answerId) {
    return new ExamAttemptAnswer(UUID.randomUUID(), examAttemptId, questionId, answerId);
  }

  public UUID getId() {
    return id;
  }

  public UUID getExamAttemptId() {
    return examAttemptId;
  }

  public UUID getQuestionId() {
    return questionId;
  }

  public UUID getAnswerId() {
    return answerId;
  }
}
