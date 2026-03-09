package com.akdemya.application.service;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.GenerateQuizUseCase;
import com.akdemya.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class GenerateQuizService implements GenerateQuizUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateQuizService.class);

    private final SourceDocumentRepository documentRepo;
    private final SourceChunkRepository chunkRepo;
    private final UnitRepository unitRepo;
    private final GeneratedQuestionDraftRepository draftRepo;
    private final QuestionGeneratorPort generator;
    private final RagProperties props;

    public GenerateQuizService(SourceDocumentRepository documentRepo,
                                SourceChunkRepository chunkRepo,
                                UnitRepository unitRepo,
                                GeneratedQuestionDraftRepository draftRepo,
                                QuestionGeneratorPort generator,
                                RagProperties props) {
        this.documentRepo = documentRepo;
        this.chunkRepo = chunkRepo;
        this.unitRepo = unitRepo;
        this.draftRepo = draftRepo;
        this.generator = generator;
        this.props = props;
    }

    @Override
    @Transactional
    public GenerateQuizResult generate(GenerateQuizCommand command) {
        SourceDocument doc = documentRepo.findById(command.sourceId())
                .orElseThrow(() -> new NoSuchElementException("Source document not found: " + command.sourceId()));

        if (doc.getStatus() != SourceDocument.Status.PROCESSED) {
            throw new IllegalStateException("Document is not processed yet. Status: " + doc.getStatus());
        }

        Unit unit = unitRepo.findById(command.unitId())
                .orElseThrow(() -> new NoSuchElementException("Unit not found: " + command.unitId()));

        List<SourceChunk> contextChunks = chunkRepo.findByUnitId(command.unitId());
        if (contextChunks.size() > props.getRetrievalTopK()) {
            contextChunks = contextChunks.subList(0, props.getRetrievalTopK());
        }
        log.info("Retrieved {} context chunks for unit='{}' sourceId={}", contextChunks.size(), unit.getName(), command.sourceId());

        if (contextChunks.isEmpty()) {
            log.warn("No chunks found for unitId={}", command.unitId());
            return new GenerateQuizResult(List.of());
        }

        List<GeneratedQuestionDraft> drafts = generator.generate(command, contextChunks, unit.getName());
        log.info("LLM generated {} question drafts for unit='{}'", drafts.size(), unit.getName());

        if (command.storeAsDraft() && !drafts.isEmpty()) {
            drafts = draftRepo.saveAll(drafts);
            log.info("Stored {} drafts for sourceId={}", drafts.size(), command.sourceId());
        }

        return new GenerateQuizResult(drafts);
    }
}
