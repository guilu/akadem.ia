package com.akdemya.adapter.infrastructure.ai;

import com.akdemya.application.config.AiProperties;
import com.akdemya.domain.port.out.EmbeddingPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI embeddings adapter.
 *
 * Config required:
 *   app.ai.api-key=sk-...
 *   app.ai.embedding-model=text-embedding-3-small  (default)
 *   app.ai.base-url=https://api.openai.com/v1      (default)
 *
 * No API key → throws at call time with a clear message.
 */
@Component
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingAdapter.class);

    private final AiProperties props;
    private final RestClient restClient;

    public OpenAiEmbeddingAdapter(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        validateApiKey();
        log.debug("Requesting embeddings for {} texts, model={}", texts.size(), props.getEmbeddingModel());

        Map<String, Object> body = Map.of(
                "input", texts,
                "model", props.getEmbeddingModel()
        );

        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new RuntimeException("Empty response from OpenAI embeddings API");
        }

        return response.data().stream()
                .sorted((a, b) -> Integer.compare(a.index(), b.index()))
                .map(EmbeddingData::embedding)
                .toList();
    }

    private void validateApiKey() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key not configured. Set app.ai.api-key in application.properties or AI_API_KEY env var.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(int index, float[] embedding) {}
}
