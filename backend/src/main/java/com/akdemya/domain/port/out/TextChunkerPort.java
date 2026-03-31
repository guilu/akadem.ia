package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceChunk;
import com.akdemya.domain.model.SourceDocument;

import java.util.List;

/**
 * Splits raw document text into semantic chunks with metadata.
 * Implementations should try semantic (structural) chunking first and
 * fall back to size-based chunking with overlap when no structure is found.
 */
public interface TextChunkerPort {
    List<SourceChunk> chunk(String text, SourceDocument document);
}
