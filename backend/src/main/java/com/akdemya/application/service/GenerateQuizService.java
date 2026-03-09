package com.akdemya.application.service;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.GeneratedQuestionDraft;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
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
    private final GeneratedQuestionDraftRepository draftRepo;
    private final EmbeddingPort embedding;
    private final VectorSearchPort vectorSearch;
    private final QuestionGeneratorPort generator;
    private final RagProperties props;

    public GenerateQuizService(SourceDocumentRepository documentRepo,
                                GeneratedQuestionDraftRepository draftRepo,
                                EmbeddingPort embedding,
                                VectorSearchPort vectorSearch,
                                QuestionGeneratorPort generator,
                                RagProperties props) {
        this.documentRepo = documentRepo;
        this.draftRepo = draftRepo;
        this.embedding = embedding;
        this.vectorSearch = vectorSearch;
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

        float[] queryVector = embedding.embed(command.topic());
        List<SourceChunk> contextChunks = vectorSearch.findTopK(queryVector, command.sourceId(), props.getRetrievalTopK());
        log.info("Retrieved {} context chunks for topic='{}' sourceId={}", contextChunks.size(), command.topic(), command.sourceId());

        if (contextChunks.isEmpty()) {
            log.warn("No relevant context found for topic='{}' sourceId={}", command.topic(), command.sourceId());
            return new GenerateQuizResult(List.of());
        }

        List<GeneratedQuestionDraft> drafts = generator.generate(command, contextChunks);
        log.info("LLM generated {} question drafts for topic='{}'", drafts.size(), command.topic());

        if (command.storeAsDraft() && !drafts.isEmpty()) {
            drafts = draftRepo.saveAll(drafts);
            log.info("Stored {} drafts for sourceId={}", drafts.size(), command.sourceId());
        }

        return new GenerateQuizResult(drafts);
    }

    @Override
    public List<GeneratedQuestionDraft> listDrafts(UUID sourceId) {
        return draftRepo.findBySourceDocumentId(sourceId);
    }
}
