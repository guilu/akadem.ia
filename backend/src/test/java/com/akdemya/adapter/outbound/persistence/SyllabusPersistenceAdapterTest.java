package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SyllabusEntity;
import com.akdemya.adapter.outbound.persistence.mapper.SyllabusMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataSyllabusRepository;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SyllabusPersistenceAdapterTest {

  private final SpringDataSyllabusRepository repository = mock(SpringDataSyllabusRepository.class);
  private final SyllabusMapper mapper = new SyllabusMapper();
  private final SyllabusPersistenceAdapter adapter = new SyllabusPersistenceAdapter(repository, mapper);

  @Test
  void findAllReturnsMappedSyllabuses() {
    SyllabusEntity entity = new SyllabusEntity("Math", "desc");
    entity.setId(UUID.randomUUID());
    entity.setVisibility("GLOBAL");
    when(repository.findAll()).thenReturn(List.of(entity));

    List<Syllabus> result = adapter.findAll();

    assertEquals(1, result.size());
    assertEquals("Math", result.get(0).getName());
  }

  @Test
  void findByIdReturnsMappedSyllabus() {
    UUID id = UUID.randomUUID();
    SyllabusEntity entity = new SyllabusEntity("Physics", "desc");
    entity.setId(id);
    entity.setVisibility("GLOBAL");
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    Optional<Syllabus> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals(id, result.get().getId());
    assertEquals("Physics", result.get().getName());
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    Optional<Syllabus> result = adapter.findById(id);

    assertFalse(result.isPresent());
  }

  @Test
  void saveSyllabusPersistsGlobalVisibility() {
    Syllabus syllabus = Syllabus.createGlobal("Chemistry", "desc");

    SyllabusEntity savedEntity = new SyllabusEntity(syllabus.getName(), syllabus.getDescription());
    savedEntity.setId(syllabus.getId());
    savedEntity.setVisibility("GLOBAL");
    savedEntity.setOwnerId(null);

    when(repository.save(any())).thenReturn(savedEntity);

    Syllabus saved = adapter.save(syllabus);

    assertEquals(Visibility.GLOBAL, saved.getVisibility());
    assertNull(saved.getOwnerId());
    verify(repository).save(argThat(e -> "GLOBAL".equals(e.getVisibility())));
  }

  @Test
  void saveSyllabusPersistsPrivateVisibilityAndOwnerId() {
    UUID ownerId = UUID.randomUUID();
    Syllabus syllabus = Syllabus.createPrivate("My Syllabus", "desc", ownerId);

    SyllabusEntity savedEntity = new SyllabusEntity(syllabus.getName(), syllabus.getDescription());
    savedEntity.setId(syllabus.getId());
    savedEntity.setVisibility("PRIVATE");
    savedEntity.setOwnerId(ownerId);

    when(repository.save(any())).thenReturn(savedEntity);

    Syllabus saved = adapter.save(syllabus);

    assertEquals(Visibility.PRIVATE, saved.getVisibility());
    assertEquals(ownerId, saved.getOwnerId());
    verify(repository).save(argThat(e -> "PRIVATE".equals(e.getVisibility()) && ownerId.equals(e.getOwnerId())));
  }

  @Test
  void deleteByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(repository).deleteById(id);
  }

  @Test
  void findVisibleByUserIdReturnsGlobalAndOwnedPrivate() {
    UUID userId = UUID.randomUUID();

    SyllabusEntity global = new SyllabusEntity("Global Syllabus", "desc");
    global.setId(UUID.randomUUID());
    global.setVisibility("GLOBAL");
    global.setOwnerId(null);

    SyllabusEntity privateOwned = new SyllabusEntity("My Syllabus", "desc");
    privateOwned.setId(UUID.randomUUID());
    privateOwned.setVisibility("PRIVATE");
    privateOwned.setOwnerId(userId);

    when(repository.findByVisibilityOrOwnerId(userId)).thenReturn(List.of(global, privateOwned));

    List<Syllabus> result = adapter.findVisibleByUserId(userId);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.GLOBAL));
    assertTrue(result.stream().anyMatch(s -> s.getVisibility() == Visibility.PRIVATE && s.getOwnerId().equals(userId)));
  }
}
