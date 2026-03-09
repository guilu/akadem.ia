package com.akdemya.application.service;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.port.in.GenerateQuizUseCase;
import com.akdemya.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateQuizServiceTest {

    private final UUID sourceId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();

    private StubSourceDocumentRepository documentRepo;
    private StubGeneratedQuestionDraftRepository draftRepo;
    private StubEmbeddingPort embeddingPort;
    private StubVectorSearchPort vectorSearchPort;
    private StubQuestionGeneratorPort generatorPort;
    private RagProperties props;
    private GenerateQuizService service;

    @BeforeEach
    void setUp() {
        props = new RagProperties();
        props.setRetrievalTopK(5);

        documentRepo = new StubSourceDocumentRepository();
        draftRepo = new StubGeneratedQuestionDraftRepository();
        embeddingPort = new StubEmbeddingPort();
        vectorSearchPort = new StubVectorSearchPort();
        generatorPort = new StubQuestionGeneratorPort();

        service = new GenerateQuizService(documentRepo, draftRepo, embeddingPort, vectorSearchPort, generatorPort, props);
    }

    @Test
    void generateReturnsAndStoresDrafts() {
        SourceDocument doc = new SourceDocument(sourceId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);

        SourceChunk chunk = SourceChunk.create(sourceId, "El Rey es el Jefe del Estado.", 0, null);
        vectorSearchPort.addChunk(chunk);

        GeneratedQuestionDraft mockDraft = GeneratedQuestionDraft.create(
                sourceId, unitId, "La Corona", "MEDIUM",
                "¿Quién es el Jefe del Estado?",
                List.of("El Rey", "El Presidente", "El Parlamento", "El Gobierno"),
                0, null, "Artículo 56 CE", "Artículo 56"
        );
        generatorPort.addDraft(mockDraft);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, "La Corona", Question.Difficulty.MEDIUM, 1, false, true
        );

        GenerateQuizUseCase.GenerateQuizResult result = service.generate(cmd);

        assertEquals(1, result.drafts().size());
        assertEquals(1, draftRepo.saved.size()); // storeAsDraft=true
    }

    @Test
    void generateDoesNotStoreWhenStorageDisabled() {
        SourceDocument doc = new SourceDocument(sourceId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);

        SourceChunk chunk = SourceChunk.create(sourceId, "Contenido", 0, null);
        vectorSearchPort.addChunk(chunk);

        GeneratedQuestionDraft mockDraft = GeneratedQuestionDraft.create(
                sourceId, unitId, "Tema", "EASY",
                "Pregunta?", List.of("A", "B", "C", "D"), 0, null, null, null
        );
        generatorPort.addDraft(mockDraft);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, "Tema", Question.Difficulty.EASY, 1, false, false
        );

        service.generate(cmd);

        assertTrue(draftRepo.saved.isEmpty()); // storeAsDraft=false
    }

    @Test
    void generateThrowsWhenDocumentNotFound() {
        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                UUID.randomUUID(), null, "Tema", Question.Difficulty.EASY, 1, false, false
        );
        assertThrows(NoSuchElementException.class, () -> service.generate(cmd));
    }

    @Test
    void generateThrowsWhenDocumentNotProcessed() {
        SourceDocument doc = new SourceDocument(sourceId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.UPLOADED, "/tmp/test.pdf");
        documentRepo.save(doc);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, null, "Tema", Question.Difficulty.EASY, 1, false, false
        );
        assertThrows(IllegalStateException.class, () -> service.generate(cmd));
    }

    @Test
    void generateReturnsEmptyWhenNoContextChunks() {
        SourceDocument doc = new SourceDocument(sourceId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);
        // No chunks in vectorSearchPort

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, null, "Tema", Question.Difficulty.EASY, 1, false, false
        );

        GenerateQuizUseCase.GenerateQuizResult result = service.generate(cmd);

        assertTrue(result.drafts().isEmpty());
    }

    @Test
    void commandValidationRejectsNullSourceId() {
        assertThrows(IllegalArgumentException.class, () ->
                new GenerateQuizUseCase.GenerateQuizCommand(null, null, "Tema", Question.Difficulty.EASY, 1, false, false)
        );
    }

    @Test
    void commandValidationRejectsExcessiveQuestionCount() {
        assertThrows(IllegalArgumentException.class, () ->
                new GenerateQuizUseCase.GenerateQuizCommand(sourceId, null, "Tema", Question.Difficulty.EASY, 100, false, false)
        );
    }

    // --- Stubs ---

    static class StubSourceDocumentRepository implements SourceDocumentRepository {
        private final Map<UUID, SourceDocument> store = new HashMap<>();

        @Override public SourceDocument save(SourceDocument d) { store.put(d.getId(), d); return d; }
        @Override public Optional<SourceDocument> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<SourceDocument> findAll() { return List.copyOf(store.values()); }
    }

    static class StubGeneratedQuestionDraftRepository implements GeneratedQuestionDraftRepository {
        final List<GeneratedQuestionDraft> saved = new ArrayList<>();

        @Override public GeneratedQuestionDraft save(GeneratedQuestionDraft d) { saved.add(d); return d; }
        @Override public List<GeneratedQuestionDraft> saveAll(List<GeneratedQuestionDraft> ds) { saved.addAll(ds); return ds; }
        @Override public List<GeneratedQuestionDraft> findBySourceDocumentId(UUID id) {
            return saved.stream().filter(d -> d.getSourceDocumentId().equals(id)).toList();
        }
    }

    static class StubEmbeddingPort implements EmbeddingPort {
        @Override public float[] embed(String text) { return new float[]{0.1f, 0.2f, 0.3f}; }
        @Override public List<float[]> embedBatch(List<String> texts) {
            return texts.stream().map(t -> new float[]{0.1f, 0.2f, 0.3f}).toList();
        }
    }

    static class StubVectorSearchPort implements VectorSearchPort {
        private final List<SourceChunk> chunks = new ArrayList<>();
        void addChunk(SourceChunk c) { chunks.add(c); }

        @Override public List<SourceChunk> findTopK(float[] queryVector, UUID sourceDocumentId, int topK) {
            return chunks.stream().filter(c -> c.getSourceDocumentId().equals(sourceDocumentId))
                    .limit(topK).toList();
        }
    }

    static class StubQuestionGeneratorPort implements QuestionGeneratorPort {
        private final List<GeneratedQuestionDraft> drafts = new ArrayList<>();
        void addDraft(GeneratedQuestionDraft d) { drafts.add(d); }

        @Override public List<GeneratedQuestionDraft> generate(
                GenerateQuizUseCase.GenerateQuizCommand command, List<SourceChunk> contextChunks) {
            return List.copyOf(drafts);
        }
    }
}
