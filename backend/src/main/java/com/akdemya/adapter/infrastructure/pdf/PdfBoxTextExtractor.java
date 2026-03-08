package com.akdemya.adapter.infrastructure.pdf;

import com.akdemya.domain.port.out.SourceTextExtractorPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts and normalizes text from PDF documents using Apache PDFBox.
 * Extensible: add other implementations (e.g., DocxTextExtractor) and dispatch
 * from SourceDocumentService based on contentType.
 */
@Component
public class PdfBoxTextExtractor implements SourceTextExtractorPort {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxTextExtractor.class);

    @Override
    public String extract(InputStream inputStream, String contentType) {
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String raw = stripper.getText(doc);
            return normalize(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
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
