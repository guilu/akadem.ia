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

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI chat completions adapter for question generation.
 *
 * Uses a strict JSON-only system prompt.
 * Validates each generated question before accepting it.
 * Invalid questions are logged and discarded.
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
    private final RestClient restClient;

    public OpenAiQuestionGeneratorAdapter(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Override
    public List<GeneratedQuestionDraft> generate(GenerateQuizUseCase.GenerateQuizCommand command,
                                                  List<SourceChunk> contextChunks) {
        validateApiKey();

        String context = buildContext(contextChunks);
        String userPrompt = buildUserPrompt(command, context);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getChatModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("response_format", Map.of("type", "json_object"));

        log.info("Calling OpenAI chat completions, model={}, topic='{}', questionCount={}",
                props.getChatModel(), command.topic(), command.questionCount());

        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("Empty response from OpenAI chat completions API");
        }

        String rawJson = response.choices().get(0).message().content();
        return parseAndValidate(rawJson, command);
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

    private List<GeneratedQuestionDraft> parseAndValidate(String rawJson, GenerateQuizUseCase.GenerateQuizCommand command) {
        QuizJson parsed;
        try {
            parsed = JSON.readValue(rawJson, QuizJson.class);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON response: {}", e.getMessage());
            log.debug("Raw LLM response: {}", rawJson);
            throw new RuntimeException("LLM returned non-parseable JSON: " + e.getMessage(), e);
        }

        if (parsed.questions() == null) return List.of();

        List<GeneratedQuestionDraft> drafts = new ArrayList<>();
        for (QuestionJson q : parsed.questions()) {
            Optional<String> error = validateQuestion(q);
            if (error.isPresent()) {
                log.warn("Discarding invalid question: {} - {}", error.get(), q.statement());
                continue;
            }
            List<String> answerTexts = q.answers().stream().map(AnswerJson::text).toList();
            drafts.add(GeneratedQuestionDraft.create(
                    command.sourceId(),
                    command.unitId(),
                    command.topic(),
                    command.difficulty().name(),
                    q.statement().trim(),
                    answerTexts,
                    q.correctIndex(),
                    q.hint(),
                    q.explanation(),
                    q.reference()
            ));
        }
        log.info("Accepted {}/{} questions after validation", drafts.size(), parsed.questions().size());
        return drafts;
    }

    private Optional<String> validateQuestion(QuestionJson q) {
        if (q.statement() == null || q.statement().isBlank()) return Optional.of("empty statement");
        if (q.answers() == null || q.answers().size() != 4) return Optional.of("must have exactly 4 answers");
        if (q.correctIndex() < 0 || q.correctIndex() > 3) return Optional.of("correctIndex out of range");

        for (AnswerJson a : q.answers()) {
            if (a.text() == null || a.text().isBlank()) return Optional.of("answer text is empty");
        }

        long distinct = q.answers().stream()
                .map(a -> a.text().trim().toLowerCase())
                .distinct().count();
        if (distinct < 4) return Optional.of("duplicate answers detected");

        return Optional.empty();
    }

    private void validateApiKey() {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key not configured. Set app.ai.api-key or AI_API_KEY env var.");
        }
    }

    // --- JSON DTOs (internal to this adapter) ---

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
