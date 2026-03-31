package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.UnitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FlashcardImportExportService implements FlashcardImportExportUseCase {

  private final FlashcardManagementUseCase managementUseCase;
  private final UnitRepository unitRepository;
  private final ObjectMapper objectMapper;

  public FlashcardImportExportService(FlashcardManagementUseCase managementUseCase,
                                      UnitRepository unitRepository,
                                      ObjectMapper objectMapper) {
    this.managementUseCase = managementUseCase;
    this.unitRepository = unitRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public ImportResult importFlashcards(UUID unitId, String format, String content) {
    List<String[]> rows;
    List<String> errors = new ArrayList<>();

    try {
      rows = "json".equalsIgnoreCase(format) ? parseJson(content) : parseCsv(content);
    } catch (Exception e) {
      return new ImportResult(0, 0, List.of("Error al parsear el archivo: " + e.getMessage()));
    }

    int imported = 0;
    int skipped = 0;

    for (int i = 0; i < rows.size(); i++) {
      String[] row = rows.get(i);
      String front = row.length > 0 ? row[0].trim() : "";
      String back = row.length > 1 ? row[1].trim() : "";

      if (front.isEmpty() && back.isEmpty()) {
        skipped++;
        continue;
      }
      if (front.isEmpty()) {
        errors.add("Fila " + (i + 1) + ": el anverso está vacío");
        skipped++;
        continue;
      }
      if (back.isEmpty()) {
        errors.add("Fila " + (i + 1) + ": el reverso está vacío");
        skipped++;
        continue;
      }

      try {
        managementUseCase.createFlashcard(
            new FlashcardManagementUseCase.CreateCommand(unitId, front, back));
        imported++;
      } catch (Exception e) {
        errors.add("Fila " + (i + 1) + ": " + e.getMessage());
        skipped++;
      }
    }

    return new ImportResult(imported, skipped, errors);
  }

  @Override
  public String exportFlashcards(UUID unitId, String format) {
    List<Flashcard> flashcards = managementUseCase.listByUnit(unitId);

    if ("json".equalsIgnoreCase(format)) {
      List<Map<String, String>> list = flashcards.stream()
          .map(f -> Map.of("front", f.getFront(), "back", f.getBack()))
          .toList();
      try {
        return objectMapper.writeValueAsString(list);
      } catch (Exception e) {
        throw new RuntimeException("Error al generar JSON", e);
      }
    }

    StringBuilder sb = new StringBuilder("front,back\n");
    for (Flashcard f : flashcards) {
      sb.append(csvEscape(f.getFront())).append(',').append(csvEscape(f.getBack())).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String exportFlashcardsBySubject(UUID subjectId, String format) {
    List<Flashcard> flashcards = unitRepository.findBySubjectId(subjectId).stream()
        .flatMap(unit -> managementUseCase.listByUnit(unit.getId()).stream())
        .toList();

    if ("json".equalsIgnoreCase(format)) {
      List<Map<String, String>> list = flashcards.stream()
          .map(f -> Map.of("front", f.getFront(), "back", f.getBack()))
          .toList();
      try {
        return objectMapper.writeValueAsString(list);
      } catch (Exception e) {
        throw new RuntimeException("Error al generar JSON", e);
      }
    }

    StringBuilder sb = new StringBuilder("front,back\n");
    for (Flashcard f : flashcards) {
      sb.append(csvEscape(f.getFront())).append(',').append(csvEscape(f.getBack())).append('\n');
    }
    return sb.toString();
  }

  private List<String[]> parseCsv(String content) {
    List<String[]> rows = new ArrayList<>();
    String[] lines = content.split("\r?\n", -1);
    boolean firstLine = true;

    for (String line : lines) {
      if (line.isBlank()) continue;
      if (firstLine) {
        firstLine = false;
        String normalized = line.strip().toLowerCase().replace("\"", "");
        if (normalized.equals("front,back")) continue;
      }
      rows.add(parseCsvLine(line));
    }
    return rows;
  }

  private String[] parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == '"') {
          inQuotes = true;
        } else if (c == ',') {
          fields.add(current.toString());
          current = new StringBuilder();
        } else {
          current.append(c);
        }
      }
    }
    fields.add(current.toString());
    return fields.toArray(new String[0]);
  }

  private List<String[]> parseJson(String content) throws Exception {
    List<Map<String, String>> list = objectMapper.readValue(
        content, new TypeReference<>() {});
    List<String[]> rows = new ArrayList<>();
    for (Map<String, String> item : list) {
      rows.add(new String[]{
          item.getOrDefault("front", ""),
          item.getOrDefault("back", "")
      });
    }
    return rows;
  }

  private String csvEscape(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
