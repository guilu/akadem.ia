package com.akdemya.application.service;

import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.out.AnswerRepository;
import com.akdemya.domain.port.out.QuestionRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.UnitRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentManagementVisibilityTest {

  private final SubjectRepository subjectRepo = mock(SubjectRepository.class);
  private final UnitRepository unitRepo = mock(UnitRepository.class);
  private final QuestionRepository questionRepo = mock(QuestionRepository.class);
  private final AnswerRepository answerRepo = mock(AnswerRepository.class);

  private final com.akdemya.domain.port.out.SyllabusRepository syllabusRepo = mock(com.akdemya.domain.port.out.SyllabusRepository.class);
  private final ContentManagement service = new ContentManagement(subjectRepo, unitRepo, questionRepo, answerRepo, syllabusRepo);

  @Test
  void getVisibleSubjectsReturnsGlobalAndCallerPrivate() {
    UUID userId = UUID.randomUUID();
    UUID ownerId = userId;

    Subject global = Subject.createGlobal("Global Math", "desc");
    Subject privateOwned = Subject.createPrivate("My Math", "desc", ownerId);
    Subject otherPrivate = Subject.createPrivate("Other Math", "desc", UUID.randomUUID());

    when(subjectRepo.findVisibleByUserId(userId)).thenReturn(List.of(global, privateOwned));

    List<Subject> result = service.getVisibleSubjects(userId);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.GLOBAL));
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.PRIVATE && ownerId.equals(s.getOwnerId())));
    assertFalse(result.stream().anyMatch(s -> s == otherPrivate));
  }

  @Test
  void getAllSubjectsStillWorks() {
    Subject global = Subject.createGlobal("Math", "desc");
    when(subjectRepo.findAll()).thenReturn(List.of(global));

    List<Subject> result = service.getAllSubjects();

    assertEquals(1, result.size());
  }

  @Test
  void createSubjectWithVisibilityAndOwnerSavesCorrectly() {
    UUID userId = UUID.randomUUID();
    Subject toCreate = Subject.createPrivate("My Math", "desc", userId);
    when(subjectRepo.save(any())).thenReturn(toCreate);

    Subject saved = service.createSubject(toCreate);

    assertEquals(Visibility.PRIVATE, saved.getVisibility());
    assertEquals(userId, saved.getOwnerId());
    verify(subjectRepo).save(toCreate);
  }
}
