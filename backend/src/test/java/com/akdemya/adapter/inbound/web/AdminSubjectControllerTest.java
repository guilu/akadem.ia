package com.akdemya.adapter.inbound.web;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminSubjectControllerTest {

  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);

  private final AdminSubjectController controller =
      new AdminSubjectController(subjectRepo, unitRepo);

  @Test
  void listReturnsSubjectsWithUnitCount() {
    Subject s = Subject.createGlobal("Math", "desc");
    when(subjectRepo.findAll()).thenReturn(List.of(s));
    when(unitRepo.findBySubjectId(s.getId())).thenReturn(List.of());

    var result = controller.list();

    assertEquals(1, result.size());
    assertEquals("Math", result.get(0).name());
    assertEquals(0, result.get(0).unitCount());
    assertEquals(Visibility.GLOBAL, result.get(0).visibility());
  }

  @Test
  void createWithValidNameReturns200() {
    Subject saved = Subject.createGlobal("Physics", "desc");
    when(subjectRepo.save(any())).thenReturn(saved);

    var req = new AdminSubjectController.SubjectRequest("Physics", "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(200, response.getStatusCodeValue());
    verify(subjectRepo).save(any());
  }

  @Test
  void createWithNullNameReturns400() {
    var req = new AdminSubjectController.SubjectRequest(null, "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(400, response.getStatusCodeValue());
    verify(subjectRepo, never()).save(any());
  }

  @Test
  void createWithBlankNameReturns400() {
    var req = new AdminSubjectController.SubjectRequest("   ", "desc");
    ResponseEntity<?> response = controller.create(req);

    assertEquals(400, response.getStatusCodeValue());
    verify(subjectRepo, never()).save(any());
  }

  @Test
  void updateWithValidDataReturns200() {
    UUID id = UUID.randomUUID();
    Subject existing = Subject.createGlobal("Old Name", "desc");
    Subject updated = new Subject(id, "New Name", "new desc", Visibility.GLOBAL, null);
    when(subjectRepo.findById(id)).thenReturn(Optional.of(existing));
    when(subjectRepo.save(any())).thenReturn(updated);
    when(unitRepo.findBySubjectId(id)).thenReturn(List.of());

    var req = new AdminSubjectController.SubjectRequest("New Name", "new desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(200, response.getStatusCodeValue());
    verify(subjectRepo).save(any());
  }

  @Test
  void updateNotFoundReturns404() {
    UUID id = UUID.randomUUID();
    when(subjectRepo.findById(id)).thenReturn(Optional.empty());

    var req = new AdminSubjectController.SubjectRequest("Name", "desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(404, response.getStatusCodeValue());
    verify(subjectRepo, never()).save(any());
  }

  @Test
  void updateWithBlankNameReturns400() {
    UUID id = UUID.randomUUID();
    Subject existing = Subject.createGlobal("Old Name", "desc");
    when(subjectRepo.findById(id)).thenReturn(Optional.of(existing));

    var req = new AdminSubjectController.SubjectRequest("", "desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(400, response.getStatusCodeValue());
    verify(subjectRepo, never()).save(any());
  }

  @Test
  void updateWithNullNameReturns400() {
    UUID id = UUID.randomUUID();
    Subject existing = Subject.createGlobal("Old Name", "desc");
    when(subjectRepo.findById(id)).thenReturn(Optional.of(existing));

    var req = new AdminSubjectController.SubjectRequest(null, "desc");
    ResponseEntity<?> response = controller.update(id, req);

    assertEquals(400, response.getStatusCodeValue());
    verify(subjectRepo, never()).save(any());
  }

  @Test
  void deleteReturns200() {
    UUID id = UUID.randomUUID();
    ResponseEntity<?> response = controller.delete(id);

    assertEquals(200, response.getStatusCodeValue());
    verify(subjectRepo).deleteById(id);
  }
}
