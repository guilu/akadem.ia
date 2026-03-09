package com.akdemya.adapter.infrastructure.chunking;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.out.TextChunkerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic chunker for Spanish legal/administrative documents.
 *
 * Strategy:
 *  1. Try to split by article patterns ("Artículo N", "Art. N", "ARTÍCULO N", "Sección N")
 *  2. If fewer than 3 splits found, fall back to size-based chunking with overlap
 *
 * Metadata per chunk: {"article":"62","section":"Título II","sourceDocumentId":"..."}
 */
@Component
public class SemanticChunker implements TextChunkerPort {

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "(?m)^\\s*(Art[ií]culo|Art\\.?|ARTÍCULO|Sección|SECCIÓN|Capítulo|CAPÍTULO)\\s+(\\d+[\\w.-]*).*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final int MIN_SEMANTIC_SPLITS = 3;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RagProperties props;

    public SemanticChunker(RagProperties props) {
        this.props = props;
    }

    @Override
    public List<SourceChunk> chunk(String text, UUID sourceDocumentId) {
        List<Split> splits = findSemanticSplits(text);

        if (splits.size() >= MIN_SEMANTIC_SPLITS) {
            return buildSemanticChunks(text, splits, sourceDocumentId);
        }
        return buildSizeChunks(text, sourceDocumentId);
    }

    // --- Semantic splitting ---

    private List<Split> findSemanticSplits(String text) {
        List<Split> splits = new ArrayList<>();
        Matcher m = ARTICLE_PATTERN.matcher(text);
        while (m.find()) {
            splits.add(new Split(m.start(), m.group(1), m.group(2)));
        }
        return splits;
    }

    private List<SourceChunk> buildSemanticChunks(String text, List<Split> splits, UUID sourceDocumentId) {
        List<SourceChunk> chunks = new ArrayList<>();

        // Text before first article
        if (splits.get(0).start() > 0) {
            String preamble = text.substring(0, splits.get(0).start()).trim();
            if (!preamble.isBlank()) {
                chunks.add(SourceChunk.create(sourceDocumentId, preamble, 0, metadata(sourceDocumentId, null, null)));
            }
        }

        for (int i = 0; i < splits.size(); i++) {
            int start = splits.get(i).start();
            int end = (i + 1 < splits.size()) ? splits.get(i + 1).start() : text.length();
            String content = text.substring(start, end).trim();

            if (content.isBlank()) continue;

            // If the article content is very long, sub-chunk it by size
            if (content.length() > props.getChunkSize() * 2) {
                List<SourceChunk> sub = splitBySize(content, sourceDocumentId, chunks.size(),
                        splits.get(i).keyword(), splits.get(i).number());
                chunks.addAll(sub);
            } else {
                String meta = metadata(sourceDocumentId, splits.get(i).keyword(), splits.get(i).number());
                chunks.add(SourceChunk.create(sourceDocumentId, content, chunks.size(), meta));
            }
        }

        return chunks;
    }

    // --- Size-based fallback ---

    private List<SourceChunk> buildSizeChunks(String text, UUID sourceDocumentId) {
        return splitBySize(text, sourceDocumentId, 0, null, null);
    }

    private List<SourceChunk> splitBySize(String text, UUID sourceDocumentId,
                                           int startIndex, String section, String article) {
        List<SourceChunk> chunks = new ArrayList<>();
        int chunkSize = props.getChunkSize();
        int overlap = props.getChunkOverlap();
        int pos = 0;
        int idx = startIndex;

        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());

            // Try to break at a sentence boundary to avoid mid-sentence splits
            if (end < text.length()) {
                int boundary = findSentenceBoundary(text, pos, end);
                if (boundary > pos) end = boundary;
            }

            String content = text.substring(pos, end).trim();
            if (!content.isBlank()) {
                chunks.add(SourceChunk.create(sourceDocumentId, content, idx++,
                        metadata(sourceDocumentId, section, article)));
            }

            if (end >= text.length()) break; // reached the end — no more chunks

            int nextPos = end - overlap;
            if (nextPos <= pos) nextPos = end; // safeguard: always advance
            pos = nextPos;
        }

        return chunks;
    }

    /** Find the last sentence boundary ('. ', '.\n', '? ', '! ') within the window [start, end]. */
    private int findSentenceBoundary(String text, int start, int end) {
        for (int i = end; i > start + 100; i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '?' || c == '!') && i < text.length() &&
                    (text.charAt(i) == ' ' || text.charAt(i) == '\n')) {
                return i;
            }
        }
        return end;
    }

    private String metadata(UUID sourceDocumentId, String section, String article) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("sourceDocumentId", sourceDocumentId.toString());
        if (section != null) m.put("section", section);
        if (article != null) m.put("article", article);
        try {
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record Split(int start, String keyword, String number) {}
}
