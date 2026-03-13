package com.akdemya.adapter.infrastructure.pdf;

import com.akdemya.domain.port.out.SourceTextExtractorPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extracts and normalizes text from PDF documents using Apache PDFBox.
 * Includes TOC (table of contents) section detection and removal to avoid
 * polluting the semantic chunking pipeline with index entries.
 */
@Component
public class PdfBoxTextExtractor implements SourceTextExtractorPort {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxTextExtractor.class);

    // A TOC line: some text followed by 3+ dots/spaces and a page number at the end
    private static final Pattern TOC_LINE_PATTERN = Pattern.compile(
            "^.{3,}[.\\s]{3,}\\d{1,3}\\s*$"
    );

    // Minimum consecutive TOC lines to consider a block as TOC
    private static final int TOC_BLOCK_THRESHOLD = 4;

    @Override
    public String extract(InputStream inputStream, String contentType) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String raw = stripper.getText(doc);
            String normalized = normalize(raw);
            String withoutToc = stripTocSections(normalized);
            log.debug("Extracted {} chars (after TOC strip: {} chars)", normalized.length(), withoutToc.length());
            return withoutToc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Removes table-of-contents sections from extracted text.
     * A TOC section is a contiguous block of ≥4 lines that each match the
     * pattern: <text> <3+ dots or spaces> <page number>.
     */
    private String stripTocSections(String text) {
        String[] lines = text.split("\n", -1);
        boolean[] isTocLine = new boolean[lines.length];

        // Mark individual TOC lines
        for (int i = 0; i < lines.length; i++) {
            isTocLine[i] = TOC_LINE_PATTERN.matcher(lines[i]).matches();
        }

        // Find contiguous blocks of TOC lines meeting the threshold
        boolean[] inTocBlock = new boolean[lines.length];
        int i = 0;
        while (i < lines.length) {
            if (isTocLine[i]) {
                int j = i;
                while (j < lines.length && isTocLine[j]) j++;
                if (j - i >= TOC_BLOCK_THRESHOLD) {
                    for (int k = i; k < j; k++) inTocBlock[k] = true;
                }
                i = j;
            } else {
                i++;
            }
        }

        // Rebuild text excluding TOC block lines
        int removedLines = 0;
        List<String> kept = new ArrayList<>(lines.length);
        for (int k = 0; k < lines.length; k++) {
            if (inTocBlock[k]) {
                removedLines++;
            } else {
                kept.add(lines[k]);
            }
        }

        if (removedLines > 0) {
            log.info("Stripped {} TOC lines from extracted text", removedLines);
        }

        return String.join("\n", kept);
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text
                // Remove control chars except newline and tab
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                // Normalize Windows line endings
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                // Replace 3+ consecutive newlines with 2
                .replaceAll("\n{3,}", "\n\n")
                // Replace multiple spaces/tabs with single space
                .replaceAll("[ \\t]+", " ")
                // Trim trailing whitespace on each line
                .replaceAll("(?m) +$", "")
                .trim();
    }
}
