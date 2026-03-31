package com.akdemya.adapter.infrastructure.chunking;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SemanticChunkerTest {

    private SemanticChunker chunker;
    private final UUID docId = UUID.randomUUID();
    private SourceDocument doc;

    @BeforeEach
    void setUp() {
        RagProperties props = new RagProperties();
        props.setChunkSize(500);
        props.setChunkOverlap(100);
        chunker = new SemanticChunker(props);
        doc = new SourceDocument(docId, UUID.randomUUID(), "TestDoc.pdf", "PDF", "1", "hash", LocalDateTime.now(), SourceDocument.Status.PROCESSED, "path");
    }

    @Test
    void semanticChunkingByArticle() {
        String text = """
                Preámbulo del documento.

                Artículo 1. El Rey es el Jefe del Estado.
                El Rey arbitra y modera el funcionamiento regular de las instituciones.

                Artículo 2. La soberanía nacional reside en el pueblo español.
                Del pueblo español emanan los poderes del Estado.

                Artículo 3. El castellano es la lengua oficial del Estado.
                Todos los españoles tienen el deber de conocerla y el derecho a usarla.
                """;

        List<SourceChunk> chunks = chunker.chunk(text, doc);

        assertTrue(chunks.size() >= 3, "Should produce at least 3 chunks for 3 articles");
        assertTrue(chunks.stream().anyMatch(c -> c.getContent().contains("Artículo 1")));
        assertTrue(chunks.stream().anyMatch(c -> c.getContent().contains("Artículo 2")));
        assertTrue(chunks.stream().anyMatch(c -> c.getContent().contains("Artículo 3")));
    }

    @Test
    void fallbackToSizeChunkingWhenNoStructure() {
        String text = "a".repeat(2000);
        List<SourceChunk> chunks = chunker.chunk(text, doc);
        assertTrue(chunks.size() >= 2, "Should produce multiple size-based chunks");
        for (SourceChunk c : chunks) {
            assertFalse(c.getContent().isBlank());
        }
    }

    @Test
    void eachChunkHasSourceDocumentId() {
        String text = """
                Artículo 1. Uno.
                Artículo 2. Dos.
                Artículo 3. Tres.
                """;
        List<SourceChunk> chunks = chunker.chunk(text, doc);
        for (SourceChunk c : chunks) {
            assertEquals(docId, c.getSourceDocumentId());
        }
    }

    @Test
    void chunkIndexIsSequential() {
        String text = """
                Artículo 1. Uno.
                Artículo 2. Dos.
                Artículo 3. Tres.
                """;
        List<SourceChunk> chunks = chunker.chunk(text, doc);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
        }
    }

    @Test
    void metadataContainsSourceDocumentId() {
        String text = """
                Artículo 10. Contenido del artículo diez.
                Artículo 11. Contenido del artículo once.
                Artículo 12. Contenido del artículo doce.
                """;
        List<SourceChunk> chunks = chunker.chunk(text, doc);
        for (SourceChunk c : chunks) {
            assertNotNull(c.getMetadata());
            assertTrue(c.getMetadata().contains(docId.toString()));
        }
    }

    @Test
    void emptyTextProducesNoChunks() {
        List<SourceChunk> chunks = chunker.chunk("", doc);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shortTextProducesOneChunk() {
        String text = "Texto corto sin artículos.";
        List<SourceChunk> chunks = chunker.chunk(text, doc);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0).getContent());
    }
}
