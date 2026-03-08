package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.GeneratedQuestionDraftEntity;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneratedQuestionDraftMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public GeneratedQuestionDraft toDomain(GeneratedQuestionDraftEntity e) {
        return new GeneratedQuestionDraft(
                e.getId(),
                e.getSourceDocumentId(),
                e.getUnitId(),
                e.getTopic(),
                e.getDifficulty(),
                e.getStatement(),
                deserializeAnswers(e.getAnswers()),
                e.getCorrectIndex(),
                e.getHint(),
                e.getExplanation(),
                e.getReference(),
                e.getCreatedAt(),
                GeneratedQuestionDraft.Status.valueOf(e.getStatus())
        );
    }

    public GeneratedQuestionDraftEntity toEntity(GeneratedQuestionDraft d) {
        GeneratedQuestionDraftEntity e = new GeneratedQuestionDraftEntity();
        e.setId(d.getId());
        e.setSourceDocumentId(d.getSourceDocumentId());
        e.setUnitId(d.getUnitId());
        e.setTopic(d.getTopic());
        e.setDifficulty(d.getDifficulty());
        e.setStatement(d.getStatement());
        e.setAnswers(serializeAnswers(d.getAnswers()));
        e.setCorrectIndex(d.getCorrectIndex());
        e.setHint(d.getHint());
        e.setExplanation(d.getExplanation());
        e.setReference(d.getReference());
        e.setCreatedAt(d.getCreatedAt());
        e.setStatus(d.getStatus().name());
        return e;
    }

    private List<String> deserializeAnswers(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return JSON.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String serializeAnswers(List<String> answers) {
        try {
            return JSON.writeValueAsString(answers);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
