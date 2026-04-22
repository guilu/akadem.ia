package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminSyllabusControllerTest {

  private final SyllabusRepository syllabusRepo = mock(SyllabusRepository.class);

  private final AdminSyllabusController controller = new AdminSyllabusController(syllabusRepo);

  @Test
  void listReturnsSyllabuses() {
    Syllabus s = Syllabus.createGlobal("Default", "desc");
    when(syllabusRepo.findAll()).thenReturn(List.of(s));

    var result = controller.list();

    assertEquals(1, result.size());
    assertEquals("Default", result.get(0).name());
    assertEquals(Visibility.GLOBAL, result.get(0).visibility());
  }

  @Test
  void createWithValidNameReturns200() {
    Syllabus saved = Syllabus.createGlobal("Physics", "desc");
    when(syllabusRepo.save(any())).thenReturn(saved);

    var req = new AdminSyllabusController.SyllabusRequest("Physics", "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(syllabusRepo).save(any());
  }

  @Test
  void createWithNullNameReturns400() {
    var req = new AdminSyllabusController.SyllabusRequest(null, "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(syllabusRepo, never()).save(any());
  }

  @Test
  void createWithBlankNameReturns400() {
    var req = new AdminSyllabusController.SyllabusRequest("   ", "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(syllabusRepo, never()).save(any());
  }

  @Test
  void updateWithValidDataReturns200() {
    UUID id = UUID.randomUUID();
    Syllabus existing = Syllabus.createGlobal("Old Name", "desc");
    Syllabus updated = new Syllabus(id, "New Name", "new desc", Visibility.GLOBAL, null);
    when(syllabusRepo.findById(id)).thenReturn(Optional.of(existing));
    when(syllabusRepo.save(any())).thenReturn(updated);

    var req = new AdminSyllabusController.SyllabusRequest("New Name", "new desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(syllabusRepo).save(any());
  }

  @Test
  void updateNotFoundReturns404() {
    UUID id = UUID.randomUUID();
    when(syllabusRepo.findById(id)).thenReturn(Optional.empty());

    var req = new AdminSyllabusController.SyllabusRequest("Name", "desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(syllabusRepo, never()).save(any());
  }

  @Test
  void updateWithBlankNameReturns400() {
    UUID id = UUID.randomUUID();
    Syllabus existing = Syllabus.createGlobal("Old Name", "desc");
    when(syllabusRepo.findById(id)).thenReturn(Optional.of(existing));

    var req = new AdminSyllabusController.SyllabusRequest("", "desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(syllabusRepo, never()).save(any());
  }

  @Test
  void deleteReturns200() {
    UUID id = UUID.randomUUID();
    ResponseEntity<?> response = controller.delete(id);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(syllabusRepo).deleteById(id);
  }
}
