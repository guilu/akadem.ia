package com.akdemya.application.service;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;
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
    private final UUID subjectId = UUID.randomUUID();

    private StubSourceDocumentRepository documentRepo;
    private StubSourceChunkRepository chunkRepo;
    private StubUnitRepository unitRepo;
    private StubGeneratedQuestionDraftRepository draftRepo;
    private StubQuestionGeneratorPort generatorPort;
    private RagProperties props;
    private GenerateQuizService service;

    @BeforeEach
    void setUp() {
        props = new RagProperties();
        props.setRetrievalTopK(5);

        documentRepo = new StubSourceDocumentRepository();
        chunkRepo = new StubSourceChunkRepository();
        unitRepo = new StubUnitRepository();
        draftRepo = new StubGeneratedQuestionDraftRepository();
        generatorPort = new StubQuestionGeneratorPort();

        service = new GenerateQuizService(documentRepo, chunkRepo, unitRepo, draftRepo, generatorPort, props);
    }

    @Test
    void generateReturnsAndStoresDrafts() {
        SourceDocument doc = new SourceDocument(sourceId, subjectId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);

        Unit unit = new Unit(unitId, subjectId, "La Corona", null, 0);
        unitRepo.save(unit);

        SourceChunk chunk = SourceChunk.create(sourceId, "El Rey es el Jefe del Estado.", 0, null);
        chunkRepo.addChunk(unitId, chunk);

        GeneratedQuestionDraft mockDraft = GeneratedQuestionDraft.create(
                sourceId, unitId, "La Corona", "MEDIUM",
                "¿Quién es el Jefe del Estado?",
                List.of("El Rey", "El Presidente", "El Parlamento", "El Gobierno"),
                0, null, "Artículo 56 CE", "Artículo 56"
        );
        generatorPort.addDraft(mockDraft);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, Question.Difficulty.MEDIUM, 1, false, true
        );

        GenerateQuizUseCase.GenerateQuizResult result = service.generate(cmd);

        assertEquals(1, result.drafts().size());
        assertEquals(1, draftRepo.saved.size()); // storeAsDraft=true
    }

    @Test
    void generateDoesNotStoreWhenStorageDisabled() {
        SourceDocument doc = new SourceDocument(sourceId, subjectId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);

        Unit unit = new Unit(unitId, subjectId, "Tema", null, 0);
        unitRepo.save(unit);

        SourceChunk chunk = SourceChunk.create(sourceId, "Contenido", 0, null);
        chunkRepo.addChunk(unitId, chunk);

        GeneratedQuestionDraft mockDraft = GeneratedQuestionDraft.create(
                sourceId, unitId, "Tema", "EASY",
                "Pregunta?", List.of("A", "B", "C", "D"), 0, null, null, null
        );
        generatorPort.addDraft(mockDraft);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, Question.Difficulty.EASY, 1, false, false
        );

        service.generate(cmd);

        assertTrue(draftRepo.saved.isEmpty()); // storeAsDraft=false
    }

    @Test
    void generateThrowsWhenDocumentNotFound() {
        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                UUID.randomUUID(), unitId, Question.Difficulty.EASY, 1, false, false
        );
        assertThrows(NoSuchElementException.class, () -> service.generate(cmd));
    }

    @Test
    void generateThrowsWhenDocumentNotProcessed() {
        SourceDocument doc = new SourceDocument(sourceId, subjectId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PENDING_REVIEW, "/tmp/test.pdf");
        documentRepo.save(doc);

        Unit unit = new Unit(unitId, subjectId, "Tema", null, 0);
        unitRepo.save(unit);

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, Question.Difficulty.EASY, 1, false, false
        );
        assertThrows(IllegalStateException.class, () -> service.generate(cmd));
    }

    @Test
    void generateReturnsEmptyWhenNoContextChunks() {
        SourceDocument doc = new SourceDocument(sourceId, subjectId, "test.pdf", "PDF", null,
                "abc123", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "/tmp/test.pdf");
        documentRepo.save(doc);

        Unit unit = new Unit(unitId, subjectId, "Tema", null, 0);
        unitRepo.save(unit);
        // No chunks for unitId

        GenerateQuizUseCase.GenerateQuizCommand cmd = new GenerateQuizUseCase.GenerateQuizCommand(
                sourceId, unitId, Question.Difficulty.EASY, 1, false, false
        );

        GenerateQuizUseCase.GenerateQuizResult result = service.generate(cmd);

        assertTrue(result.drafts().isEmpty());
    }

    @Test
    void commandValidationRejectsNullSourceId() {
        assertThrows(IllegalArgumentException.class, () ->
                new GenerateQuizUseCase.GenerateQuizCommand(null, unitId, Question.Difficulty.EASY, 1, false, false)
        );
    }

    @Test
    void commandValidationRejectsNullUnitId() {
        assertThrows(IllegalArgumentException.class, () ->
                new GenerateQuizUseCase.GenerateQuizCommand(sourceId, null, Question.Difficulty.EASY, 1, false, false)
        );
    }

    @Test
    void commandValidationRejectsExcessiveQuestionCount() {
        assertThrows(IllegalArgumentException.class, () ->
                new GenerateQuizUseCase.GenerateQuizCommand(sourceId, unitId, Question.Difficulty.EASY, 100, false, false)
        );
    }

    // --- Stubs ---

    static class StubSourceDocumentRepository implements SourceDocumentRepository {
        private final Map<UUID, SourceDocument> store = new HashMap<>();

        @Override public SourceDocument save(SourceDocument d) { store.put(d.getId(), d); return d; }
        @Override public Optional<SourceDocument> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<SourceDocument> findAll() { return List.copyOf(store.values()); }
        @Override public List<SourceDocument> findBySubjectId(UUID subjectId) {
            return store.values().stream().filter(d -> subjectId.equals(d.getSubjectId())).toList();
        }
        @Override  public Optional<SourceDocument> findBySubjectIdAndName(UUID subjectId, String name) {return Optional.empty();}
        @Override public void deleteById(UUID id) {}
    }

    static class StubSourceChunkRepository implements SourceChunkRepository {
        private final Map<UUID, List<SourceChunk>> byUnitId = new HashMap<>();

        void addChunk(UUID unitId, SourceChunk chunk) {
            byUnitId.computeIfAbsent(unitId, k -> new ArrayList<>()).add(chunk);
        }

        @Override public SourceChunk save(SourceChunk chunk, float[] embedding) { return chunk; }
        @Override public SourceChunk saveWithoutEmbedding(SourceChunk chunk) { return chunk; }
        @Override public void updateUnitId(UUID chunkId, UUID unitId) {}
        @Override public List<SourceChunk> findBySourceDocumentId(UUID sourceDocumentId) { return List.of(); }
        @Override public List<SourceChunk> findByUnitId(UUID unitId) {
            return byUnitId.getOrDefault(unitId, List.of());
        }
        @Override public List<ChunkWithEmbedding> findWithEmbeddingsBySourceDocumentId(UUID sourceDocumentId) {
            return List.of();
        }
    }

    static class StubUnitRepository implements UnitRepository {
        private final Map<UUID, Unit> store = new HashMap<>();

        @Override public Unit save(Unit u) { store.put(u.getId(), u); return u; }
        @Override public Optional<Unit> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Unit> findBySubjectId(UUID subjectId) { return List.of(); }
        @Override public List<Unit> findBySubjectIdWithFlashcards(UUID subjectId) { return List.of(); }
        @Override public List<Unit> findAll() { return List.copyOf(store.values()); }
        @Override public List<Unit> findAllWithFlashcards() { return List.of(); }
        @Override public void deleteById(UUID id) { store.remove(id); }
    }

    static class StubGeneratedQuestionDraftRepository implements GeneratedQuestionDraftRepository {
        final List<GeneratedQuestionDraft> saved = new ArrayList<>();

        @Override public GeneratedQuestionDraft save(GeneratedQuestionDraft d) { saved.add(d); return d; }
        @Override public List<GeneratedQuestionDraft> saveAll(List<GeneratedQuestionDraft> ds) { saved.addAll(ds); return ds; }
        @Override public Optional<GeneratedQuestionDraft> findById(UUID id) {
            return saved.stream().filter(d -> d.getId().equals(id)).findFirst();
        }
        @Override public List<GeneratedQuestionDraft> findBySourceDocumentId(UUID id, GeneratedQuestionDraft.Status status) {
            return saved.stream().filter(d -> d.getSourceDocumentId().equals(id)).toList();
        }
        @Override public GeneratedQuestionDraft updateStatus(UUID id, GeneratedQuestionDraft.Status newStatus) {
            return saved.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
        }
    }

    static class StubQuestionGeneratorPort implements QuestionGeneratorPort {
        private final List<GeneratedQuestionDraft> drafts = new ArrayList<>();
        void addDraft(GeneratedQuestionDraft d) { drafts.add(d); }

        @Override public List<GeneratedQuestionDraft> generate(
                GenerateQuizUseCase.GenerateQuizCommand command, List<SourceChunk> contextChunks, String topic) {
            return List.copyOf(drafts);
        }
    }
}
