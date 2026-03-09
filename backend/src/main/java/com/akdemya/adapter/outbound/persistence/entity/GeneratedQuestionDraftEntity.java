package com.akdemya.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_question_drafts")
public class GeneratedQuestionDraftEntity {

    @Id
    private UUID id;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(length = 500)
    private String topic;

    @Column(length = 50)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    // JSON array of answer strings: ["answer1","answer2","answer3","answer4"]
    @Column(nullable = false, columnDefinition = "text")
    private String answers;

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;

    @Column(columnDefinition = "text")
    private String hint;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(length = 500)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 50)
    private String status;

    public GeneratedQuestionDraftEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(UUID sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
