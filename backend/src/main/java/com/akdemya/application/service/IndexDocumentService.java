package com.akdemya.application.service;

import com.akdemya.adapter.infrastructure.chunking.SemanticChunker;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.IndexSourceUseCase;
import com.akdemya.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IndexDocumentService implements IndexSourceUseCase {

    private static final Logger log = LoggerFactory.getLogger(IndexDocumentService.class);

    private final SourceDocumentRepository documentRepo;
    private final SourceChunkRepository chunkRepo;
    private final UnitRepository unitRepo;
    private final FileStoragePort storage;
    private final SourceTextExtractorPort extractor;
    private final TextChunkerPort chunker;

    public IndexDocumentService(SourceDocumentRepository documentRepo,
                                 SourceChunkRepository chunkRepo,
                                 UnitRepository unitRepo,
                                 FileStoragePort storage,
                                 SourceTextExtractorPort extractor,
                                 TextChunkerPort chunker) {
        this.documentRepo = documentRepo;
        this.chunkRepo = chunkRepo;
        this.unitRepo = unitRepo;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
    }

    @Override
    @Transactional
    public UploadPreview upload(UploadCommand command) {
        String checksum = sha256(command.bytes());
        Path storedPath = storage.store(command.filename(), command.bytes());

        SourceDocument doc = SourceDocument.create(
                command.subjectId(),
                command.filename(),
                resolveType(command.contentType()),
                null,
                checksum,
                storedPath.toString()
        );
        doc = documentRepo.save(doc);
        log.info("Source document saved as PENDING_REVIEW: id={} name={}", doc.getId(), doc.getName());

        List<SourceChunk> chunks;
        try {
            InputStream is = new ByteArrayInputStream(command.bytes());
            String rawText = extractor.extract(is, command.contentType());
            log.info("Extracted {} chars from document id={}", rawText.length(), doc.getId());

            chunks = chunker.chunk(rawText, doc.getId());
            log.info("Created {} chunks for document id={}", chunks.size(), doc.getId());

            for (SourceChunk chunk : chunks) {
                chunkRepo.saveWithoutEmbedding(chunk);
            }
        } catch (Exception e) {
            log.error("Failed to process document id={}: {}", doc.getId(), e.getMessage(), e);
            documentRepo.save(doc.withStatus(SourceDocument.Status.FAILED));
            return new UploadPreview(doc.withStatus(SourceDocument.Status.FAILED), List.of());
        }

        List<String> unitNames = SemanticChunker.extractDetectedUnitNames(chunks);
        List<DetectedUnit> detectedUnits = buildDetectedUnits(unitNames, chunks);
        log.info("Detected {} units for document id={}", detectedUnits.size(), doc.getId());

        return new UploadPreview(doc, detectedUnits);
    }

    @Override
    @Transactional
    public ConfirmResult confirm(ConfirmCommand command) {
        SourceDocument doc = documentRepo.findById(command.documentId())
                .orElseThrow(() -> new NoSuchElementException("Source document not found: " + command.documentId()));

        List<SourceChunk> allChunks = chunkRepo.findBySourceDocumentId(doc.getId());
        log.info("Confirming index for document id={}, {} chunks, {} approved units",
                doc.getId(), allChunks.size(), command.approvedUnits().size());

        // Group chunks by unitName for fast lookup
        Map<String, List<SourceChunk>> chunksByUnitName = allChunks.stream()
                .filter(c -> c.getUnitName() != null)
                .collect(Collectors.groupingBy(SourceChunk::getUnitName));

        List<Unit> savedUnits = new ArrayList<>();
        int orderIndex = 0;

        for (ApprovedUnit approvedUnit : command.approvedUnits()) {
            Unit unit = Unit.create(doc.getSubjectId(), approvedUnit.name(),
                    approvedUnit.description(), orderIndex++);
            unit = unitRepo.save(unit);
            savedUnits.add(unit);

            List<SourceChunk> matching = chunksByUnitName.getOrDefault(approvedUnit.headingKey(), List.of());
            for (SourceChunk chunk : matching) {
                chunkRepo.updateUnitId(chunk.getId(), unit.getId());
            }
            log.info("Assigned {} chunks to unit '{}' (id={})", matching.size(), unit.getName(), unit.getId());
        }

        SourceDocument processed = doc.withStatus(SourceDocument.Status.PROCESSED);
        processed = documentRepo.save(processed);
        log.info("Document id={} marked as PROCESSED with {} units", processed.getId(), savedUnits.size());

        return new ConfirmResult(processed, savedUnits);
    }

    @Override
    public List<SourceDocument> listAll() {
        return documentRepo.findAll();
    }

    @Override
    public List<SourceDocument> listBySubject(UUID subjectId) {
        return documentRepo.findBySubjectId(subjectId);
    }

    @Override
    public SourceDocument findById(UUID id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Source document not found: " + id));
    }

    private List<DetectedUnit> buildDetectedUnits(List<String> unitNames, List<SourceChunk> chunks) {
        Map<String, Long> countByName = chunks.stream()
                .filter(c -> c.getUnitName() != null)
                .collect(Collectors.groupingBy(SourceChunk::getUnitName, Collectors.counting()));

        return unitNames.stream()
                .map(name -> new DetectedUnit(name, name, countByName.getOrDefault(name, 0L).intValue()))
                .collect(Collectors.toList());
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
