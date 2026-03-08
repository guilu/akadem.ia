package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SourceChunkEntity;
import com.akdemya.domain.model.SourceChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SourceChunkMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Float>> FLOAT_LIST = new TypeReference<>() {};

    public SourceChunk toDomain(SourceChunkEntity e) {
        return new SourceChunk(e.getId(), e.getSourceDocumentId(), e.getContent(),
                e.getChunkIndex(), e.getMetadata());
    }

    public float[] embeddingFromEntity(SourceChunkEntity e) {
        if (e.getEmbedding() == null || e.getEmbedding().isBlank()) return new float[0];
        try {
            List<Float> list = JSON.readValue(e.getEmbedding(), FLOAT_LIST);
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (JsonProcessingException ex) {
            return new float[0];
        }
    }

    public SourceChunkEntity toEntity(SourceChunk d, float[] embedding) {
        SourceChunkEntity e = new SourceChunkEntity();
        e.setId(d.getId());
        e.setSourceDocumentId(d.getSourceDocumentId());
        e.setContent(d.getContent());
        e.setChunkIndex(d.getChunkIndex());
        e.setMetadata(d.getMetadata());
        e.setEmbedding(serializeEmbedding(embedding));
        return e;
    }

    private String serializeEmbedding(float[] embedding) {
        if (embedding == null || embedding.length == 0) return null;
        try {
            Float[] boxed = new Float[embedding.length];
            for (int i = 0; i < embedding.length; i++) boxed[i] = embedding[i];
            return JSON.writeValueAsString(boxed);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
