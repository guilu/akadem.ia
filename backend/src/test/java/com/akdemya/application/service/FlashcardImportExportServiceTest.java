package com.akdemya.application.service;

import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.port.in.FlashcardImportExportUseCase;
import com.akdemya.domain.port.in.FlashcardManagementUseCase;
import com.akdemya.domain.port.out.UnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class FlashcardImportExportServiceTest {

  private final FlashcardManagementUseCase managementUseCase = mock(FlashcardManagementUseCase.class);
  private final UnitRepository unitRepository = mock(UnitRepository.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final FlashcardImportExportService service =
      new FlashcardImportExportService(managementUseCase, unitRepository, objectMapper);

  private Flashcard flashcard(UUID unitId, String front, String back) {
    return new Flashcard(UUID.randomUUID(), unitId, front, back,
        LocalDateTime.now(), LocalDateTime.now());
  }

  private Unit unit(UUID subjectId) {
    return new Unit(UUID.randomUUID(), subjectId, "Unit", "", 1);
  }

  // --- exportFlashcardsBySubject: CSV ---

  @Test
  void exportFlashcardsBySubject_csvMergesAllUnits() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit1 = unit(subjectId);
    Unit unit2 = unit(subjectId);
    Flashcard card1 = flashcard(unit1.getId(), "Hello", "Hola");
    Flashcard card2 = flashcard(unit2.getId(), "Goodbye", "Adiós");

    when(unitRepository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of(unit1, unit2));
    when(managementUseCase.listVisibleByUnit(unit1.getId(), userId)).thenReturn(List.of(card1));
    when(managementUseCase.listVisibleByUnit(unit2.getId(), userId)).thenReturn(List.of(card2));

    String result = service.exportFlashcardsBySubject(subjectId, userId, "csv");

    assertTrue(result.startsWith("front,back\n"));
    assertTrue(result.contains("Hello,Hola"));
    assertTrue(result.contains("Goodbye,Adiós"));
  }

  @Test
  void exportFlashcardsBySubject_jsonMergesAllUnits() throws Exception {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit1 = unit(subjectId);
    Unit unit2 = unit(subjectId);
    Flashcard card1 = flashcard(unit1.getId(), "Cat", "Gato");
    Flashcard card2 = flashcard(unit2.getId(), "Dog", "Perro");

    when(unitRepository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of(unit1, unit2));
    when(managementUseCase.listVisibleByUnit(unit1.getId(), userId)).thenReturn(List.of(card1));
    when(managementUseCase.listVisibleByUnit(unit2.getId(), userId)).thenReturn(List.of(card2));

    String result = service.exportFlashcardsBySubject(subjectId, userId, "json");

    var parsed = objectMapper.readValue(result, List.class);
    assertEquals(2, parsed.size());
  }

  @Test
  void exportFlashcardsBySubject_noUnitsReturnsCsvHeader() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(unitRepository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of());

    String result = service.exportFlashcardsBySubject(subjectId, userId, "csv");

    assertEquals("front,back\n", result);
  }

  @Test
  void exportFlashcardsBySubject_noUnitsReturnsEmptyJsonArray() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(unitRepository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of());

    String result = service.exportFlashcardsBySubject(subjectId, userId, "json");

    assertEquals("[]", result);
  }

  @Test
  void exportFlashcardsBySubject_unitsWithNoFlashcardsReturnsCsvHeader() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit1 = unit(subjectId);

    when(unitRepository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of(unit1));
    when(managementUseCase.listVisibleByUnit(unit1.getId(), userId)).thenReturn(List.of());

    String result = service.exportFlashcardsBySubject(subjectId, userId, "csv");

    assertEquals("front,back\n", result);
  }

  // --- exportFlashcards (by unit) ---

  @Test
  void exportFlashcards_csvFormatReturnsHeaderAndRows() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Flashcard card = flashcard(unitId, "Question", "Answer");
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    String result = service.exportFlashcards(unitId, "csv", userId, false);

    assertTrue(result.startsWith("front,back\n"));
    assertTrue(result.contains("Question,Answer"));
  }

  @Test
  void exportFlashcards_jsonFormatReturnsJsonArray() throws Exception {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Flashcard card = flashcard(unitId, "Front", "Back");
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    String result = service.exportFlashcards(unitId, "json", userId, false);

    var parsed = objectMapper.readValue(result, List.class);
    assertEquals(1, parsed.size());
  }

  @Test
  void exportFlashcards_csvEscapesCommasInFields() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Flashcard card = flashcard(unitId, "A, B", "C, D");
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    String result = service.exportFlashcards(unitId, "csv", userId, false);

    assertTrue(result.contains("\"A, B\""));
    assertTrue(result.contains("\"C, D\""));
  }

  @Test
  void exportFlashcards_csvEscapesQuotesInFields() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Flashcard card = flashcard(unitId, "Say \"hello\"", "reply");
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of(card));

    String result = service.exportFlashcards(unitId, "csv", userId, false);

    assertTrue(result.contains("\"Say \"\"hello\"\"\""));
  }

  @Test
  void exportFlashcards_emptyUnitReturnsCsvHeader() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(managementUseCase.listVisibleByUnit(unitId, userId)).thenReturn(List.of());

    String result = service.exportFlashcards(unitId, "csv", userId, false);

    assertEquals("front,back\n", result);
  }

  // --- importFlashcards: CSV ---

  // --- importFlashcards: CSV ---

  @Test
  void importFlashcards_csvImportsValidRows() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\nHello,Hola\nGoodbye,Adiós\n";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(2, result.imported());
    assertEquals(0, result.skipped());
    assertTrue(result.errors().isEmpty());
    verify(managementUseCase, times(2)).createFlashcardWithVisibility(any());
  }

  @Test
  void importFlashcards_nonAdminImportsAsPrivate() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\nHello,Hola\n";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    service.importFlashcards(unitId, "csv", csv, userId, false);

    verify(managementUseCase).createFlashcardWithVisibility(
        argThat(cmd -> cmd.visibility() == com.akdemya.domain.model.Visibility.PRIVATE
            && userId.equals(cmd.ownerId())));
  }

  @Test
  void importFlashcards_adminImportsAsGlobal() {
    UUID unitId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    String csv = "front,back\nHello,Hola\n";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    service.importFlashcards(unitId, "csv", csv, adminId, true);

    verify(managementUseCase).createFlashcardWithVisibility(
        argThat(cmd -> cmd.visibility() == com.akdemya.domain.model.Visibility.GLOBAL
            && cmd.ownerId() == null));
  }

  @Test
  void importFlashcards_csvSkipsBlankRows() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\nHello,Hola\n\nGoodbye,Adiós\n";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(2, result.imported());
    assertEquals(0, result.skipped());
  }

  @Test
  void importFlashcards_csvErrorsWhenFrontIsEmpty() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\n,Hola\n";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("anverso"));
  }

  @Test
  void importFlashcards_csvErrorsWhenBackIsEmpty() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\nHello,\n";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("reverso"));
  }

  @Test
  void importFlashcards_csvSkipsBothEmptyFrontAndBack() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\n,\n";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void importFlashcards_csvHandlesCreateException() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\nHello,Hola\n";

    when(managementUseCase.createFlashcardWithVisibility(any()))
        .thenThrow(new IllegalArgumentException("duplicate"));

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("duplicate"));
  }

  @Test
  void importFlashcards_csvWithoutHeaderLineStillImports() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "Hello,Hola\nGoodbye,Adiós\n";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(2, result.imported());
  }

  @Test
  void importFlashcards_csvWithQuotedFields() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\n\"Hello, world\",\"Hola, mundo\"\n";

    Flashcard created = flashcard(unitId, "Hello, world", "Hola, mundo");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(1, result.imported());
    assertEquals(0, result.skipped());
  }

  @Test
  void importFlashcards_csvWithEscapedQuotesInFields() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String csv = "front,back\n\"Say \"\"hi\"\"\",reply\n";

    Flashcard created = flashcard(unitId, "Say \"hi\"", "reply");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", csv, userId, false);

    assertEquals(1, result.imported());
  }

  // --- importFlashcards: JSON ---

  @Test
  void importFlashcards_jsonImportsValidRows() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String json = "[{\"front\":\"Hello\",\"back\":\"Hola\"},{\"front\":\"Bye\",\"back\":\"Adiós\"}]";

    Flashcard created = flashcard(unitId, "Hello", "Hola");
    when(managementUseCase.createFlashcardWithVisibility(any())).thenReturn(created);

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "json", json, userId, false);

    assertEquals(2, result.imported());
    assertEquals(0, result.skipped());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void importFlashcards_jsonHandlesMissingFrontKey() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String json = "[{\"back\":\"Hola\"}]";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "json", json, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("anverso"));
  }

  @Test
  void importFlashcards_jsonHandlesMissingBackKey() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String json = "[{\"front\":\"Hello\"}]";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "json", json, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.skipped());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("reverso"));
  }

  @Test
  void importFlashcards_jsonReturnsParseErrorOnInvalidJson() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String badJson = "not valid json";

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "json", badJson, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("parsear"));
  }

  @Test
  void importFlashcards_csvReturnsParseErrorOnNullContent() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    FlashcardImportExportUseCase.ImportResult result = service.importFlashcards(unitId, "csv", null, userId, false);

    assertEquals(0, result.imported());
    assertEquals(1, result.errors().size());
    assertTrue(result.errors().get(0).contains("parsear"));
  }
}
