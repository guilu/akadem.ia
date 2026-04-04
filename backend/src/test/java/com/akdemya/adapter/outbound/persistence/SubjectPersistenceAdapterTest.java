package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.adapter.outbound.persistence.mapper.SubjectMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSubjectRepository;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectPersistenceAdapterTest {

  private final SpringDataSubjectRepository repository = mock(SpringDataSubjectRepository.class);
  private final SubjectMapper mapper = new SubjectMapper();
  private final SubjectPersistenceAdapter adapter = new SubjectPersistenceAdapter(repository, mapper);

  @Test
  void findVisibleByUserIdReturnsGlobalAndOwnedPrivate() {
    UUID userId = UUID.randomUUID();
    UUID ownerId = userId;

    SubjectEntity global = new SubjectEntity("Global Math", "desc");
    global.setId(UUID.randomUUID());
    global.setVisibility("GLOBAL");
    global.setOwnerId(null);

    SubjectEntity privateOwned = new SubjectEntity("My Math", "desc");
    privateOwned.setId(UUID.randomUUID());
    privateOwned.setVisibility("PRIVATE");
    privateOwned.setOwnerId(ownerId);

    when(repository.findVisibleByUserId(userId)).thenReturn(List.of(global, privateOwned));

    List<Subject> result = adapter.findVisibleByUserId(userId);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.GLOBAL));
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.PRIVATE && s.getOwnerId().equals(ownerId)));
  }

  @Test
  void saveSubjectPersistsVisibilityAndOwnerId() {
    UUID ownerId = UUID.randomUUID();
    Subject subject = Subject.createPrivate("My Subject", "desc", ownerId);

    SubjectEntity savedEntity = new SubjectEntity(subject.getName(), subject.getDescription());
    savedEntity.setId(subject.getId());
    savedEntity.setVisibility("PRIVATE");
    savedEntity.setOwnerId(ownerId);

    when(repository.save(any())).thenReturn(savedEntity);

    Subject saved = adapter.save(subject);

    assertEquals(Visibility.PRIVATE, saved.getVisibility());
    assertEquals(ownerId, saved.getOwnerId());
    verify(repository).save(argThat(e -> "PRIVATE".equals(e.getVisibility()) && ownerId.equals(e.getOwnerId())));
  }
}
