package com.akdemya.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamAttempt {
  private final UUID id;
  private final String userEmail;
  private final OffsetDateTime startedAt;
  private OffsetDateTime finishedAt;
  private Integer totalTimeSeconds;
  private Integer score;

  public ExamAttempt(UUID id, String userEmail, OffsetDateTime startedAt, OffsetDateTime finishedAt,
      Integer totalTimeSeconds, Integer score) {
    this.id = id;
    this.userEmail = userEmail;
    this.startedAt = startedAt;
    this.finishedAt = finishedAt;
    this.totalTimeSeconds = totalTimeSeconds;
    this.score = score;
  }

  public static ExamAttempt start(String userEmail) {
    return new ExamAttempt(UUID.randomUUID(), userEmail, OffsetDateTime.now(), null, null, null);
  }

  public void finish(Integer totalTimeSeconds, Integer score) {
    this.finishedAt = OffsetDateTime.now();
    this.totalTimeSeconds = totalTimeSeconds;
    this.score = score;
  }

  public UUID getId() {
    return id;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public Integer getTotalTimeSeconds() {
    return totalTimeSeconds;
  }

  public Integer getScore() {
    return score;
  }
}
