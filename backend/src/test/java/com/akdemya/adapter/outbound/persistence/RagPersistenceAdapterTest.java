package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SourceDocumentEntity;
import com.akdemya.adapter.outbound.persistence.mapper.GeneratedQuestionDraftMapper;
import com.akdemya.adapter.outbound.persistence.mapper.SourceChunkMapper;
import com.akdemya.adapter.outbound.persistence.mapper.SourceDocumentMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataGeneratedQuestionDraftRepository;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSourceChunkRepository;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSourceDocumentRepository;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({SourceDocumentMapper.class, SourceChunkMapper.class, GeneratedQuestionDraftMapper.class})
class RagPersistenceAdapterTest {

    @Autowired SpringDataSourceDocumentRepository docJpa;
    @Autowired SpringDataSourceChunkRepository chunkJpa;
    @Autowired SpringDataGeneratedQuestionDraftRepository draftJpa;

    @Autowired SourceDocumentMapper docMapper;
    @Autowired SourceChunkMapper chunkMapper;
    @Autowired GeneratedQuestionDraftMapper draftMapper;

    private SourceDocumentPersistenceAdapter docAdapter;
    private SourceChunkPersistenceAdapter chunkAdapter;
    private GeneratedQuestionDraftPersistenceAdapter draftAdapter;

    private final UUID docId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        docAdapter = new SourceDocumentPersistenceAdapter(docJpa, docMapper);
        chunkAdapter = new SourceChunkPersistenceAdapter(chunkJpa, chunkMapper);
        draftAdapter = new GeneratedQuestionDraftPersistenceAdapter(draftJpa, draftMapper);

        // Seed a source document required by FK constraints
        SourceDocumentEntity docEntity = new SourceDocumentEntity();
        docEntity.setId(docId);
        docEntity.setName("test.pdf");
        docEntity.setType("PDF");
        docEntity.setChecksum("abc123");
        docEntity.setUploadedAt(LocalDateTime.now());
        docEntity.setStatus("PROCESSED");
        docEntity.setStoragePath("/tmp/test.pdf");
        docJpa.save(docEntity);
    }

    @Test
    void saveAndFindSourceDocument() {
        SourceDocument doc = new SourceDocument(UUID.randomUUID(), null, "doc.pdf", "PDF", null,
                "xyz789", LocalDateTime.now(), SourceDocument.Status.UPLOADED, "/tmp/doc.pdf");

        SourceDocument saved = docAdapter.save(doc);
        Optional<SourceDocument> found = docAdapter.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("doc.pdf", found.get().getName());
        assertEquals(SourceDocument.Status.UPLOADED, found.get().getStatus());
    }

    @Test
    void updateSourceDocumentStatus() {
        Optional<SourceDocument> found = docAdapter.findById(docId);
        assertTrue(found.isPresent());

        SourceDocument updated = found.get().withStatus(SourceDocument.Status.PROCESSED);
        docAdapter.save(updated);

        assertEquals(SourceDocument.Status.PROCESSED, docAdapter.findById(docId).get().getStatus());
    }

    @Test
    void saveAndFindSourceChunk() {
        SourceChunk chunk = SourceChunk.create(docId, "Artículo 1. El Rey...", 0, "{\"article\":\"1\"}");
        float[] embedding = {0.1f, 0.2f, 0.3f};

        chunkAdapter.save(chunk, embedding);
        List<SourceChunk> chunks = chunkAdapter.findBySourceDocumentId(docId);

        assertEquals(1, chunks.size());
        assertEquals("Artículo 1. El Rey...", chunks.get(0).getContent());
    }

    @Test
    void findWithEmbeddingsPreservesEmbedding() {
        SourceChunk chunk = SourceChunk.create(docId, "Contenido.", 0, null);
        float[] embedding = {1.0f, 2.0f, 3.0f};
        chunkAdapter.save(chunk, embedding);

        List<com.akdemya.domain.port.out.SourceChunkRepository.ChunkWithEmbedding> results =
                chunkAdapter.findWithEmbeddingsBySourceDocumentId(docId);

        assertEquals(1, results.size());
        float[] retrieved = results.get(0).embedding();
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f}, retrieved, 0.001f);
    }

    @Test
    void saveAllAndFindDrafts() {
        GeneratedQuestionDraft d1 = GeneratedQuestionDraft.create(
                docId, null, "Tema A", "MEDIUM",
                "¿Pregunta 1?", List.of("A", "B", "C", "D"), 0, null, null, null
        );
        GeneratedQuestionDraft d2 = GeneratedQuestionDraft.create(
                docId, null, "Tema A", "EASY",
                "¿Pregunta 2?", List.of("W", "X", "Y", "Z"), 1, "pista", "explicación", "Art. 1"
        );

        draftAdapter.saveAll(List.of(d1, d2));

        List<GeneratedQuestionDraft> found = draftAdapter.findBySourceDocumentId(docId, null);
        assertEquals(2, found.size());
    }

    @Test
    void draftAnswersRoundTrip() {
        List<String> answers = List.of("Opción A", "Opción B", "Opción C", "Opción D");
        GeneratedQuestionDraft draft = GeneratedQuestionDraft.create(
                docId, null, "Tema", "HARD", "¿Enunciado?", answers, 2, null, null, null
        );

        draftAdapter.save(draft);

        GeneratedQuestionDraft found = draftAdapter.findBySourceDocumentId(docId, null).get(0);
        assertEquals(answers, found.getAnswers());
        assertEquals(2, found.getCorrectIndex());
    }
}
