package com.akdemya.adapter.infrastructure.chunking;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.port.out.TextChunkerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Semantic chunker for Spanish legal/administrative documents.
 *
 * Strategy:
 *  1. Try to split by article/section patterns ("Artículo N", "Capítulo N", etc.)
 *  2. If fewer than 3 splits found, fall back to size-based chunking with overlap
 *
 * Each chunk gets a unitName = the full heading line (e.g. "Artículo 62. Del rey")
 * which is used to group chunks into units during the index confirmation step.
 */
@Component
public class SemanticChunker implements TextChunkerPort {

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "(?m)^\\s*(T[ií]tulo|TÍTULO|Art[ií]culo|Art\\.?|ARTÍCULO|Secci[oó]n|SECCIÓN|Cap[ií]tulo|CAPÍTULO)\\s+(\\d+[\\w.-]*)(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // High-level section keywords (preferred for detected units)
    private static final Set<String> HIGH_LEVEL_KEYWORDS = Set.of(
            "título", "titulo", "capítulo", "capitulo", "sección", "seccion",
            "TÍTULO", "CAPÍTULO", "SECCIÓN"
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
            String keyword = m.group(1);
            String number = m.group(2);
            String rest = m.group(3) != null ? m.group(3).trim() : "";
            // Build a clean heading: "Artículo 62. Del rey" → unit name
            String heading = keyword + " " + number + (rest.isBlank() ? "" : ". " + rest.replaceFirst("^[.:\\s]+", "").trim());
            if (heading.length() > 200) heading = heading.substring(0, 200);
            splits.add(new Split(m.start(), keyword, number, heading.trim()));
        }
        return splits;
    }

    private List<SourceChunk> buildSemanticChunks(String text, List<Split> splits, UUID sourceDocumentId) {
        List<SourceChunk> chunks = new ArrayList<>();

        // Text before first heading (preamble)
        if (splits.get(0).start() > 0) {
            String preamble = text.substring(0, splits.get(0).start()).trim();
            if (!preamble.isBlank()) {
                chunks.add(SourceChunk.create(sourceDocumentId, preamble, 0,
                        metadata(sourceDocumentId, null, null), null));
            }
        }

        for (int i = 0; i < splits.size(); i++) {
            int start = splits.get(i).start();
            int end = (i + 1 < splits.size()) ? splits.get(i + 1).start() : text.length();
            String content = text.substring(start, end).trim();

            if (content.isBlank()) continue;

            String unitName = splits.get(i).heading();

            // If the article content is very long, sub-chunk it by size
            if (content.length() > props.getChunkSize() * 2) {
                List<SourceChunk> sub = splitBySize(content, sourceDocumentId, chunks.size(),
                        splits.get(i).keyword(), splits.get(i).number(), unitName);
                chunks.addAll(sub);
            } else {
                String meta = metadata(sourceDocumentId, splits.get(i).keyword(), splits.get(i).number());
                chunks.add(SourceChunk.create(sourceDocumentId, content, chunks.size(), meta, unitName));
            }
        }

        return chunks;
    }

    // --- Size-based fallback ---

    private List<SourceChunk> buildSizeChunks(String text, UUID sourceDocumentId) {
        return splitBySize(text, sourceDocumentId, 0, null, null, null);
    }

    private List<SourceChunk> splitBySize(String text, UUID sourceDocumentId,
                                           int startIndex, String section, String article, String unitName) {
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
                        metadata(sourceDocumentId, section, article), unitName));
            }

            if (end >= text.length()) break;

            int nextPos = end - overlap;
            if (nextPos <= pos) nextPos = end;
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

    /**
     * Extract unique detected units from a list of chunks.
     * Prefers high-level headings (Título, Capítulo, Sección) over Artículo.
     * Returns units sorted by first occurrence.
     */
    public static List<String> extractDetectedUnitNames(List<SourceChunk> chunks) {
        // Check if high-level headings exist
        boolean hasHighLevel = chunks.stream()
                .filter(c -> c.getUnitName() != null)
                .anyMatch(c -> isHighLevelHeading(c.getUnitName()));

        // If high-level headings exist, use only those; otherwise use all headings
        return chunks.stream()
                .filter(c -> c.getUnitName() != null)
                .filter(c -> !hasHighLevel || isHighLevelHeading(c.getUnitName()))
                .map(SourceChunk::getUnitName)
                .distinct()
                .collect(Collectors.toList());
    }

    private static boolean isHighLevelHeading(String unitName) {
        if (unitName == null) return false;
        String lower = unitName.toLowerCase();
        return lower.startsWith("título") || lower.startsWith("titulo") ||
               lower.startsWith("capítulo") || lower.startsWith("capitulo") ||
               lower.startsWith("sección") || lower.startsWith("seccion");
    }

    private record Split(int start, String keyword, String number, String heading) {}
}
