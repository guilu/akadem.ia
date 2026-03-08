package com.akdemya.domain.port.out;

import java.io.InputStream;

/**
 * Extracts and normalizes text from a document input stream.
 * Implementations should normalize whitespace, control characters and line breaks.
 */
public interface SourceTextExtractorPort {
    String extract(InputStream inputStream, String contentType);
}
