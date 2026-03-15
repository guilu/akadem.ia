package com.akdemya.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface FlashcardImportExportUseCase {

  ImportResult importFlashcards(UUID unitId, String format, String content);

  String exportFlashcards(UUID unitId, String format);

  record ImportResult(int imported, int skipped, List<String> errors) {}
}
