package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_attempts")
public class ExamAttemptEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    private OffsetDateTime startedAt = OffsetDateTime.now();
    private OffsetDateTime finishedAt;
    private Integer totalTimeSeconds;
    private Integer score; // number of correct answers

    public ExamAttemptEntity() {
    }

    public ExamAttemptEntity(String userEmail, Integer totalTimeSeconds) {
        this.userEmail = userEmail;
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setFinishedAt(OffsetDateTime t) {
        this.finishedAt = t;
    }

    public void setScore(Integer s) {
        this.score = s;
    }
}
