package com.akdemya.application.service;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.port.in.SourceDocumentUseCase;
import com.akdemya.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SourceDocumentService implements SourceDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(SourceDocumentService.class);

    private final SourceDocumentRepository documentRepo;
    private final SourceChunkRepository chunkRepo;
    private final FileStoragePort storage;
    private final SourceTextExtractorPort extractor;
    private final TextChunkerPort chunker;
    private final EmbeddingPort embedding;
    private final RagProperties props;

    public SourceDocumentService(SourceDocumentRepository documentRepo,
                                  SourceChunkRepository chunkRepo,
                                  FileStoragePort storage,
                                  SourceTextExtractorPort extractor,
                                  TextChunkerPort chunker,
                                  EmbeddingPort embedding,
                                  RagProperties props) {
        this.documentRepo = documentRepo;
        this.chunkRepo = chunkRepo;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embedding = embedding;
        this.props = props;
    }

    @Override
    @Transactional
    public UploadResult upload(UploadCommand command) {
        String checksum = sha256(command.bytes());
        Path storedPath = storage.store(command.filename(), command.bytes());

        SourceDocument doc = SourceDocument.create(
                command.filename(),
                resolveType(command.contentType()),
                null,
                checksum,
                storedPath.toString()
        );
        doc = documentRepo.save(doc);
        log.info("Source document uploaded: id={} name={}", doc.getId(), doc.getName());

        try {
            doc = processDocument(doc, command.bytes(), command.contentType());
        } catch (Exception e) {
            log.error("Failed to process document id={}: {}", doc.getId(), e.getMessage(), e);
            doc = documentRepo.save(doc.withStatus(SourceDocument.Status.FAILED));
        }

        return new UploadResult(doc);
    }

    private SourceDocument processDocument(SourceDocument doc, byte[] bytes, String contentType) {
        InputStream is = new ByteArrayInputStream(bytes);
        String rawText = extractor.extract(is, contentType);
        log.info("Extracted {} chars from document id={}", rawText.length(), doc.getId());

        List<SourceChunk> chunks = chunker.chunk(rawText, doc.getId());
        log.info("Created {} chunks for document id={}", chunks.size(), doc.getId());

        List<String> contents = chunks.stream().map(SourceChunk::getContent).toList();
        List<float[]> embeddings = embedding.embedBatch(contents);

        for (int i = 0; i < chunks.size(); i++) {
            chunkRepo.save(chunks.get(i), embeddings.get(i));
        }
        log.info("Stored {} chunks with embeddings for document id={}", chunks.size(), doc.getId());

        SourceDocument processed = doc.withStatus(SourceDocument.Status.PROCESSED);
        return documentRepo.save(processed);
    }

    @Override
    public List<SourceDocument> listAll() {
        return documentRepo.findAll();
    }

    @Override
    public SourceDocument findById(UUID id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Source document not found: " + id));
    }

    private static String resolveType(String contentType) {
        if (contentType != null && contentType.contains("pdf")) return "PDF";
        return "UNKNOWN";
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }
}
