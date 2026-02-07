package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "exam_attempt_answers")
public class ExamAttemptAnswerEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;
    @Column(name = "selected_answer_id")
    private UUID selectedAnswerId;

    public ExamAttemptAnswerEntity() {
    }

    public ExamAttemptAnswerEntity(UUID attemptId, UUID questionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getSelectedAnswerId() {
        return selectedAnswerId;
    }

    public void setSelectedAnswerId(UUID a) {
        this.selectedAnswerId = a;
    }
}
