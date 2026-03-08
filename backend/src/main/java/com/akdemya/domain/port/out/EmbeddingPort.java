package com.akdemya.domain.port.out;

import java.util.List;

/**
 * Generates dense vector embeddings for text inputs.
 * Decoupled from any specific provider (OpenAI, Cohere, local models, etc.)
 */
public interface EmbeddingPort {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
