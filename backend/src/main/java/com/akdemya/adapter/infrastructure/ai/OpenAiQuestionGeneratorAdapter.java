package com.akdemya.adapter.infrastructure.ai;

import com.akdemya.application.config.AiProperties;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.in.GenerateQuizUseCase;
import com.akdemya.domain.port.out.QuestionGeneratorPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM question generator with Groq as primary provider and OpenRouter as fallback.
 *
 * Flow:
 *   1. Try Groq (fast, cheap, llama models)
 *   2. On any error → log warning and retry via OpenRouter
 *   3. If both fail → propagate the OpenRouter exception
 *
 * Both providers use the OpenAI-compatible chat completions API.
 *
 * Config required:
 *   app.ai.groq.api-key=gsk_...
 *   app.ai.openrouter.api-key=sk-or-...
 */
@Component
public class OpenAiQuestionGeneratorAdapter implements QuestionGeneratorPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiQuestionGeneratorAdapter.class);

    private static final String SYSTEM_PROMPT =
            "Eres un generador de preguntas tipo test para oposiciones españolas. " +
            "Debes usar exclusivamente el contexto proporcionado. " +
            "No inventes información que no esté en el contexto. " +
            "Si el contexto no permite formular preguntas fiables, devuelve una lista vacía. " +
            "Genera preguntas claras, no ambiguas, con exactamente 4 opciones y una sola correcta. " +
            "Devuelve ÚNICAMENTE JSON válido, sin texto adicional, sin markdown, sin explicaciones fuera del JSON.";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiProperties props;
    private final RestClient groqClient;
    private final RestClient openRouterClient;

    public OpenAiQuestionGeneratorAdapter(AiProperties props) {
        this.props = props;
        this.groqClient = RestClient.builder()
                .baseUrl(props.getGroq().getBaseUrl())
                .build();
        this.openRouterClient = RestClient.builder()
                .baseUrl(props.getOpenrouter().getBaseUrl())
                .build();
    }

    @Override
    public List<GeneratedQuestionDraft> generate(GenerateQuizUseCase.GenerateQuizCommand command,
                                                  List<SourceChunk> contextChunks) {
        validateAtLeastOneProvider();

        String context = buildContext(contextChunks);
        String userPrompt = buildUserPrompt(command, context);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        );

        if (props.getGroq().isConfigured()) {
            try {
                return callProvider(groqClient, props.getGroq().getApiKey(),
                        props.getGroq().getChatModel(), messages, command, "Groq", Map.of());
            } catch (Exception e) {
                log.warn("Groq call failed ({}), falling back to OpenRouter", e.getMessage());
            }
        }

        if (!props.getOpenrouter().isConfigured()) {
            throw new IllegalStateException("Groq failed and OpenRouter API key is not configured.");
        }
        return callProvider(openRouterClient, props.getOpenrouter().getApiKey(),
                props.getOpenrouter().getChatModel(), messages, command, "OpenRouter",
                Map.of(
                        "HTTP-Referer", props.getOpenrouter().getSiteUrl(),
                        "X-Title", props.getOpenrouter().getAppName()
                ));
    }

    private List<GeneratedQuestionDraft> callProvider(RestClient client, String apiKey,
                                                       String model,
                                                       List<Map<String, String>> messages,
                                                       GenerateQuizUseCase.GenerateQuizCommand command,
                                                       String providerName,
                                                       Map<String, String> extraHeaders) {
        log.info("Calling {} chat completions, model={}, topic='{}', questionCount={}",
                providerName, model, command.topic(), command.questionCount());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("response_format", Map.of("type", "json_object"));

        var request = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json");

        for (var entry : extraHeaders.entrySet()) {
            request = request.header(entry.getKey(), entry.getValue());
        }

        ChatResponse response = request.body(body).retrieve().body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("Empty response from " + providerName + " chat completions API");
        }

        String rawJson = response.choices().get(0).message().content();
        return parseAndValidate(rawJson, command, providerName);
    }

    private String buildContext(List<SourceChunk> chunks) {
        return chunks.stream()
                .map(c -> "[Fragmento " + c.getChunkIndex() + "]\n" + c.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String buildUserPrompt(GenerateQuizUseCase.GenerateQuizCommand command, String context) {
        return String.format("""
                Genera exactamente %d preguntas tipo test sobre el tema: "%s"
                Dificultad: %s
                %s

                Requisitos estrictos:
                - Cada pregunta debe tener exactamente 4 respuestas
                - Solo una respuesta es correcta (correctIndex: 0, 1, 2 o 3)
                - El enunciado no puede estar vacío
                - Las respuestas no pueden estar vacías ni repetidas
                - Incluye una breve explicación de por qué la respuesta correcta es correcta
                - Si el texto hace referencia a un artículo concreto, indícalo en "reference"
                %s

                Contexto del documento:
                ---
                %s
                ---

                Devuelve ÚNICAMENTE este JSON (sin texto adicional):
                {
                  "questions": [
                    {
                      "statement": "Enunciado de la pregunta",
                      "answers": [
                        {"text": "Opción A"},
                        {"text": "Opción B"},
                        {"text": "Opción C"},
                        {"text": "Opción D"}
                      ],
                      "correctIndex": 0,
                      "hint": "Pista opcional",
                      "explanation": "Explicación breve",
                      "reference": "Artículo 62"
                    }
                  ]
                }
                """,
                command.questionCount(),
                command.topic(),
                command.difficulty().name(),
                command.includeHints() ? "- Incluye una pista (hint) útil para recordar la respuesta" : "",
                command.includeHints() ? "" : "- El campo hint puede ir vacío",
                context
        );
    }

    private List<GeneratedQuestionDraft> parseAndValidate(String rawJson,
                                                           GenerateQuizUseCase.GenerateQuizCommand command,
                                                           String providerName) {
        QuizJson parsed;
        try {
            parsed = JSON.readValue(rawJson, QuizJson.class);
        } catch (Exception e) {
            log.error("[{}] Failed to parse LLM JSON response: {}", providerName, e.getMessage());
            log.debug("[{}] Raw response: {}", providerName, rawJson);
            throw new RuntimeException("LLM returned non-parseable JSON: " + e.getMessage(), e);
        }

        if (parsed.questions() == null) return List.of();

        List<GeneratedQuestionDraft> drafts = new ArrayList<>();
        for (QuestionJson q : parsed.questions()) {
            Optional<String> error = validateQuestion(q);
            if (error.isPresent()) {
                log.warn("[{}] Discarding invalid question: {} — {}", providerName, error.get(), q.statement());
                continue;
            }
            List<String> answerTexts = q.answers().stream().map(AnswerJson::text).toList();
            drafts.add(GeneratedQuestionDraft.create(
                    command.sourceId(), command.unitId(), command.topic(),
                    command.difficulty().name(),
                    q.statement().trim(), answerTexts, q.correctIndex(),
                    q.hint(), q.explanation(), q.reference()
            ));
        }
        log.info("[{}] Accepted {}/{} questions after validation",
                providerName, drafts.size(), parsed.questions().size());
        return drafts;
    }

    private Optional<String> validateQuestion(QuestionJson q) {
        if (q.statement() == null || q.statement().isBlank()) return Optional.of("empty statement");
        if (q.answers() == null || q.answers().size() != 4) return Optional.of("must have exactly 4 answers");
        if (q.correctIndex() < 0 || q.correctIndex() > 3) return Optional.of("correctIndex out of range");
        for (AnswerJson a : q.answers()) {
            if (a.text() == null || a.text().isBlank()) return Optional.of("answer text is empty");
        }
        long distinct = q.answers().stream().map(a -> a.text().trim().toLowerCase()).distinct().count();
        if (distinct < 4) return Optional.of("duplicate answers detected");
        return Optional.empty();
    }

    private void validateAtLeastOneProvider() {
        if (!props.getGroq().isConfigured() && !props.getOpenrouter().isConfigured()) {
            throw new IllegalStateException(
                    "No AI provider configured. Set GROQ_API_KEY and/or OPENROUTER_API_KEY.");
        }
    }

    // --- Internal JSON DTOs ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuizJson(List<QuestionJson> questions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuestionJson(
            String statement,
            List<AnswerJson> answers,
            @JsonProperty("correctIndex") int correctIndex,
            String hint,
            String explanation,
            String reference
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnswerJson(String text) {}
}
