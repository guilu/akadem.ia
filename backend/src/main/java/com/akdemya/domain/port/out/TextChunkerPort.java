package com.akdemya.domain.port.out;

import com.akdemya.domain.model.SourceChunk;

import java.util.List;
import java.util.UUID;

/**
 * Splits raw document text into semantic chunks with metadata.
 * Implementations should try semantic (structural) chunking first and
 * fall back to size-based chunking with overlap when no structure is found.
 */
public interface TextChunkerPort {
    List<SourceChunk> chunk(String text, UUID sourceDocumentId);
}
