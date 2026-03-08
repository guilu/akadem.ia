package com.akdemya.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GeneratedQuestionDraft {

    private final UUID id;
    private final UUID sourceDocumentId;
    private final UUID unitId;
    private final String topic;
    private final String difficulty;
    private final String statement;
    private final List<String> answers;
    private final int correctIndex;
    private final String hint;
    private final String explanation;
    private final String reference;
    private final LocalDateTime createdAt;
    private final Status status;

    public enum Status {
        GENERATED, VALIDATED, REJECTED
    }

    public GeneratedQuestionDraft(UUID id, UUID sourceDocumentId, UUID unitId,
                                  String topic, String difficulty, String statement,
                                  List<String> answers, int correctIndex,
                                  String hint, String explanation, String reference,
                                  LocalDateTime createdAt, Status status) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.unitId = unitId;
        this.topic = topic;
        this.difficulty = difficulty;
        this.statement = statement;
        this.answers = answers;
        this.correctIndex = correctIndex;
        this.hint = hint;
        this.explanation = explanation;
        this.reference = reference;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static GeneratedQuestionDraft create(UUID sourceDocumentId, UUID unitId,
                                                String topic, String difficulty,
                                                String statement, List<String> answers,
                                                int correctIndex, String hint,
                                                String explanation, String reference) {
        return new GeneratedQuestionDraft(
                UUID.randomUUID(), sourceDocumentId, unitId, topic, difficulty,
                statement, answers, correctIndex, hint, explanation, reference,
                LocalDateTime.now(), Status.GENERATED
        );
    }

    public UUID getId() { return id; }
    public UUID getSourceDocumentId() { return sourceDocumentId; }
    public UUID getUnitId() { return unitId; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public String getStatement() { return statement; }
    public List<String> getAnswers() { return answers; }
    public int getCorrectIndex() { return correctIndex; }
    public String getHint() { return hint; }
    public String getExplanation() { return explanation; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
}
