package com.akdemya.adapter.infrastructure.ai;

import com.akdemya.application.config.AiProperties;
import com.akdemya.domain.port.out.EmbeddingPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Embedding adapter using OpenRouter (OpenAI-compatible /embeddings endpoint).
 * Groq does not offer embedding models, so OpenRouter is used for this step.
 *
 * Config:
 *   app.ai.openrouter.api-key=...
 *   app.ai.openrouter.embedding-model=openai/text-embedding-3-small  (default)
 */
@Component
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingAdapter.class);

    private final AiProperties props;
    private final RestClient restClient;

    public OpenAiEmbeddingAdapter(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getOpenrouter().getBaseUrl())
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        AiProperties.OpenRouterConfig cfg = props.getOpenrouter();
        if (!cfg.isConfigured()) {
            throw new IllegalStateException(
                    "OpenRouter API key not configured. Set app.ai.openrouter.api-key or OPENROUTER_API_KEY.");
        }

        log.debug("Requesting embeddings for {} text(s) via OpenRouter, model={}", texts.size(), cfg.getEmbeddingModel());

        Map<String, Object> body = Map.of(
                "input", texts,
                "model", cfg.getEmbeddingModel()
        );

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + cfg.getApiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", cfg.getSiteUrl())
                .header("X-Title", cfg.getAppName())
                .body(body)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new RuntimeException("Empty response from OpenRouter embeddings API");
        }

        return response.data().stream()
                .sorted((a, b) -> Integer.compare(a.index(), b.index()))
                .map(EmbeddingData::embedding)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(int index, float[] embedding) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmbeddingData that = (EmbeddingData) o;
            return index == that.index && java.util.Arrays.equals(embedding, that.embedding);
        }

        @Override
        public int hashCode() {
            int result = java.util.Objects.hash(index);
            result = 31 * result + java.util.Arrays.hashCode(embedding);
            return result;
        }

        @Override
        public String toString() {
            return "EmbeddingData[" +
                   "index=" + index + ", " +
                   "embedding=" + java.util.Arrays.toString(embedding) + "]";
        }
    }
}
