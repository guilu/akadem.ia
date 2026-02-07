package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class QuestionEntity {
    @Id
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitEntity unit;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<AnswerEntity> answers = new java.util.ArrayList<>();

    @Column(columnDefinition = "text", nullable = false)
    private String text;
    private String explanation;
    private String difficulty; // EASY|MEDIUM|HARD

    public QuestionEntity() {
    }

    public QuestionEntity(UnitEntity unit, String text, String difficulty) {
        this.unit = unit;
        this.text = text;
        this.difficulty = difficulty;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUnitId() {
        return unit != null ? unit.getId() : null;
    }

    public UnitEntity getUnit() {
        return unit;
    }

    public String getText() {
        return text;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setUnit(UnitEntity u) {
        this.unit = u;
    }

    public void setText(String t) {
        this.text = t;
    }

    public void setExplanation(String e) {
        this.explanation = e;
    }

    public void setDifficulty(String d) {
        this.difficulty = d;
    }

    public java.util.List<AnswerEntity> getAnswers() {
        return answers;
    }

    public void setAnswers(java.util.List<AnswerEntity> answers) {
        this.answers = answers;
    }
}
