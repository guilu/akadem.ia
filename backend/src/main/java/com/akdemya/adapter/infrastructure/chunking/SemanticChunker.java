package com.akdemya.adapter.infrastructure.chunking;

import com.akdemya.application.config.RagProperties;
import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;
import com.akdemya.domain.port.out.TextChunkerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hierarchical chunker for Spanish legal/administrative documents.
 *
 * Strategy:
 *  1. Split text by heading patterns (Título, Capítulo, Sección, Artículo).
 *  2. Filter out splits whose content is shorter than MIN_CHUNK_CONTENT_CHARS —
 *     these are TOC remnants (heading + dots + page number).
 *  3. Assign unitName hierarchically:
 *     - High-level headings (Título/Capítulo/Sección) → define the unit;
 *       their chunks carry unitName = their own heading.
 *     - Low-level headings (Artículo) → are chunks *within* the parent unit;
 *       their chunks carry unitName = last seen Título/Capítulo heading.
 *       Fallback: if no parent unit yet, unitName = the Artículo heading.
 *  4. Large sections are sub-chunked by size with overlap.
 *  5. If fewer than 3 valid structural splits are found, fall back to
 *     size-based chunking (generic documents).
 */
@Component
public class SemanticChunker implements TextChunkerPort {

    private static final Logger log = LoggerFactory.getLogger(SemanticChunker.class);

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "(?m)^\\s*(T[ií]tulo|TÍTULO|Art[ií]culo|Art\\.?|ARTÍCULO|Secci[oó]n|SECCIÓN|Cap[ií]tulo|CAPÍTULO)\\s+(\\d+[\\w.-]*)(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Set<String> HIGH_LEVEL_KEYWORDS = new HashSet<>(List.of(
            "título", "titulo", "capítulo", "capitulo", "sección", "seccion"
    ));

    private static final int MIN_SEMANTIC_SPLITS = 3;

    /** Chunks shorter than this are considered TOC remnants and are discarded. */
    private static final int MIN_CHUNK_CONTENT_CHARS = 50;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RagProperties props;

    public SemanticChunker(RagProperties props) {
        this.props = props;
    }

    @Override
    public List<SourceChunk> chunk(String text, SourceDocument document) {
        List<Split> splits = findSemanticSplits(text);

        // Filter splits whose content block is too short (TOC remnants)
        List<Split> validSplits = filterShortSplits(splits, text);

        log.info("Found {} total splits, {} valid after short-content filter", splits.size(), validSplits.size());

        if (validSplits.size() >= MIN_SEMANTIC_SPLITS) {
            return buildHierarchicalChunks(text, validSplits, document.getId());
        }
        return buildSizeChunks(text, document);
    }

    // -------------------------------------------------------------------------
    // Semantic splitting
    // -------------------------------------------------------------------------

    private List<Split> findSemanticSplits(String text) {
        List<Split> splits = new ArrayList<>();
        Matcher m = ARTICLE_PATTERN.matcher(text);
        while (m.find()) {
            String keyword = m.group(1);
            String number = m.group(2);
            String rest = m.group(3) != null ? m.group(3).trim() : "";
            String heading = keyword + " " + number + (rest.isBlank() ? "" : ". " + rest.replaceFirst("^[.:\\s]+", "").trim());
            if (heading.length() > 200) heading = heading.substring(0, 200);
            splits.add(new Split(m.start(), keyword, number, heading.trim()));
        }
        return splits;
    }

    /**
     * Remove splits whose content (from this split to the next) is shorter than
     * MIN_CHUNK_CONTENT_CHARS. These are index/TOC entries that survived the
     * extractor-level TOC strip.
     */
    private List<Split> filterShortSplits(List<Split> splits, String text) {
        if (splits.isEmpty()) return splits;
        List<Split> valid = new ArrayList<>();
        for (int i = 0; i < splits.size(); i++) {
            int start = splits.get(i).start();
            int end = (i + 1 < splits.size()) ? splits.get(i + 1).start() : text.length();
            String content = text.substring(start, end).trim();
            if (content.length() >= MIN_CHUNK_CONTENT_CHARS) {
                valid.add(splits.get(i));
            } else {
                log.debug("Discarding short split '{}' ({} chars)", splits.get(i).heading(), content.length());
            }
        }
        return valid;
    }

    // -------------------------------------------------------------------------
    // Hierarchical chunking
    // -------------------------------------------------------------------------

    /**
     * Builds chunks with hierarchical unit assignment:
     * - Título/Capítulo/Sección splits update the "current unit" and their
     *   own content block carries unitName = their heading.
     * - Artículo splits carry unitName = the last seen high-level heading
     *   (or their own heading if no high-level heading has been seen yet).
     */
    private List<SourceChunk> buildHierarchicalChunks(String text, List<Split> splits, UUID sourceDocumentId) {
        List<SourceChunk> chunks = new ArrayList<>();
        String currentUnitName = null;

        // Preamble: text before the first split
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

            boolean isHighLevel = isHighLevelKeyword(splits.get(i).keyword());
            String unitName;

            if (isHighLevel) {
                // This heading defines a new unit
                currentUnitName = splits.get(i).heading();
                unitName = currentUnitName;
            } else {
                // Artículo: belongs to the current high-level unit
                unitName = (currentUnitName != null) ? currentUnitName : splits.get(i).heading();
            }

            // Sub-chunk large content blocks
            if (content.length() > props.getChunkSize() * 2) {
                List<SourceChunk> sub = splitBySize(content, sourceDocumentId, chunks.size(),
                        splits.get(i).keyword(), splits.get(i).number(), unitName);
                chunks.addAll(sub);
            } else {
                String meta = metadata(sourceDocumentId, splits.get(i).keyword(), splits.get(i).number());
                chunks.add(SourceChunk.create(sourceDocumentId, content, chunks.size(), meta, unitName));
            }
        }

        log.info("Built {} hierarchical chunks for document {}", chunks.size(), sourceDocumentId);
        return chunks;
    }

    // -------------------------------------------------------------------------
    // Size-based fallback
    // -------------------------------------------------------------------------

    private List<SourceChunk> buildSizeChunks(String text, SourceDocument document) {
        log.info("Using size-based fallback chunking for document {}", document.getId());
        String documentName = document.getName();
        if (documentName != null && documentName.toLowerCase().endsWith(".pdf")) {
            documentName = documentName.substring(0, documentName.length() - 4);
        }
        return splitBySize(text, document.getId(), 0, null, null, documentName);
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

    private int findSentenceBoundary(String text, int start, int end) {
        // Try paragraph boundaries first (\n\n)
        for (int i = end; i > start + 100; i--) {
            if (text.charAt(i - 1) == '\n' && i < text.length() && text.charAt(i) == '\n') {
                return i;
            }
        }
        // Try line boundaries (\n)
        for (int i = end; i > start + 100; i--) {
            if (text.charAt(i - 1) == '\n') {
                return i;
            }
        }
        // Try sentence boundaries
        for (int i = end; i > start + 100; i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '?' || c == '!') && i < text.length() &&
                    (text.charAt(i) == ' ' || text.charAt(i) == '\n')) {
                return i;
            }
        }
        return end;
    }

    // -------------------------------------------------------------------------
    // Unit name extraction (for upload preview)
    // -------------------------------------------------------------------------

    /**
     * Extract unique detected units from a list of chunks.
     * With hierarchical chunking, Artículo chunks already carry their parent
     * Título/Capítulo as unitName, so high-level units dominate naturally.
     * Returns units sorted by first occurrence.
     */
    public static List<String> extractDetectedUnitNames(List<SourceChunk> chunks) {
        boolean hasHighLevel = chunks.stream()
                .filter(c -> c.getUnitName() != null)
                .anyMatch(c -> isHighLevelHeading(c.getUnitName()));

        return chunks.stream()
                .filter(c -> c.getUnitName() != null)
                .filter(c -> !hasHighLevel || isHighLevelHeading(c.getUnitName()))
                .map(SourceChunk::getUnitName)
                .distinct()
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isHighLevelKeyword(String keyword) {
        if (keyword == null) return false;
        String lower = keyword.toLowerCase();
        return lower.startsWith("tít") || lower.startsWith("tit") ||
               lower.startsWith("cap") || lower.startsWith("secc") || lower.startsWith("seción");
    }

    private static boolean isHighLevelHeading(String unitName) {
        if (unitName == null) return false;
        String lower = unitName.toLowerCase();
        return lower.startsWith("título") || lower.startsWith("titulo") ||
               lower.startsWith("capítulo") || lower.startsWith("capitulo") ||
               lower.startsWith("sección") || lower.startsWith("seccion");
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

    private record Split(int start, String keyword, String number, String heading) {}
}
