package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "answers")
public class AnswerEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(columnDefinition = "text", nullable = false)
    private String text;
    @Column(nullable = false)
    private boolean correct = false;

    public AnswerEntity() {
    }

    public AnswerEntity(QuestionEntity question, String text, boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuestionId() {
        return question != null ? question.getId() : null;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setQuestion(QuestionEntity q) {
        this.question = q;
    }

    public void setText(String t) {
        this.text = t;
    }

    public void setCorrect(boolean c) {
        this.correct = c;
    }
}
