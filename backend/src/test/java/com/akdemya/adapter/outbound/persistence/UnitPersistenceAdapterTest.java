package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import com.akdemya.adapter.outbound.persistence.mapper.UnitMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataUnitRepository;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UnitPersistenceAdapterTest {

  private final SpringDataUnitRepository repository = mock(SpringDataUnitRepository.class);
  private final UnitMapper mapper = new UnitMapper();
  private final UnitPersistenceAdapter adapter = new UnitPersistenceAdapter(repository, mapper);

  private UnitEntity globalUnitEntity(UUID subjectId, String name) {
    UnitEntity entity = new UnitEntity();
    entity.setId(UUID.randomUUID());
    entity.setName(name);
    entity.setDescription("desc");
    entity.setOrderIndex(1);
    entity.setVisibility("GLOBAL");
    SubjectEntity subject = new SubjectEntity(name + "-parent", "desc");
    subject.setId(subjectId);
    entity.setSubject(subject);
    return entity;
  }

  private UnitEntity privateUnitEntity(UUID subjectId, String name, UUID ownerId) {
    UnitEntity entity = globalUnitEntity(subjectId, name);
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);
    return entity;
  }

  @Test
  void findBySubjectIdReturnsMappedUnits() {
    UUID subjectId = UUID.randomUUID();
    UnitEntity entity = globalUnitEntity(subjectId, "Algebra");
    when(repository.findBySubject_IdOrderByOrderIndexAscNameAsc(subjectId)).thenReturn(List.of(entity));

    List<Unit> result = adapter.findBySubjectId(subjectId);

    assertEquals(1, result.size());
    assertEquals("Algebra", result.get(0).getName());
    assertEquals(Visibility.GLOBAL, result.get(0).getVisibility());
  }

  @Test
  void findByIdReturnsMappedUnit() {
    UUID id = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    UnitEntity entity = globalUnitEntity(subjectId, "Calculus");
    entity.setId(id);
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    Optional<Unit> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals(id, result.get().getId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    Optional<Unit> result = adapter.findById(id);

    assertTrue(result.isEmpty());
  }

  @Test
  void deleteByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(repository).deleteById(id);
  }

  @Test
  void findVisibleBySubjectIdAndUserIdReturnsMixedVisibility() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UnitEntity global = globalUnitEntity(subjectId, "Global Unit");
    UnitEntity priv = privateUnitEntity(subjectId, "Private Unit", userId);
    when(repository.findVisibleBySubjectIdAndUserId(subjectId, userId)).thenReturn(List.of(global, priv));

    List<Unit> result = adapter.findVisibleBySubjectIdAndUserId(subjectId, userId);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(u -> u.getVisibility() == Visibility.GLOBAL));
    assertTrue(result.stream().anyMatch(u -> u.getVisibility() == Visibility.PRIVATE));
  }

  @Test
  void findBySubjectIdAndScopeNullReturnsAll() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UnitEntity global = globalUnitEntity(subjectId, "G");
    UnitEntity priv = privateUnitEntity(subjectId, "P", userId);
    when(repository.findAllBySubjectIdAndScope(subjectId, userId)).thenReturn(List.of(global, priv));

    List<Unit> result = adapter.findBySubjectIdAndScope(subjectId, userId, null);

    assertEquals(2, result.size());
    verify(repository).findAllBySubjectIdAndScope(subjectId, userId);
  }

  @Test
  void findBySubjectIdAndScopeGlobalReturnsOnlyGlobal() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UnitEntity global = globalUnitEntity(subjectId, "G");
    when(repository.findGlobalBySubjectId(subjectId)).thenReturn(List.of(global));

    List<Unit> result = adapter.findBySubjectIdAndScope(subjectId, userId, Visibility.GLOBAL);

    assertEquals(1, result.size());
    assertEquals(Visibility.GLOBAL, result.get(0).getVisibility());
    verify(repository).findGlobalBySubjectId(subjectId);
  }

  @Test
  void findBySubjectIdAndScopePrivateReturnsOnlyOwned() {
    UUID subjectId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UnitEntity priv = privateUnitEntity(subjectId, "P", userId);
    when(repository.findPrivateBySubjectIdAndOwner(subjectId, userId)).thenReturn(List.of(priv));

    List<Unit> result = adapter.findBySubjectIdAndScope(subjectId, userId, Visibility.PRIVATE);

    assertEquals(1, result.size());
    assertEquals(Visibility.PRIVATE, result.get(0).getVisibility());
    verify(repository).findPrivateBySubjectIdAndOwner(subjectId, userId);
  }

  @Test
  void findAllReturnsMappedUnits() {
    UUID subjectId = UUID.randomUUID();
    UnitEntity entity = globalUnitEntity(subjectId, "U1");
    when(repository.findAll()).thenReturn(List.of(entity));

    List<Unit> result = adapter.findAll();

    assertEquals(1, result.size());
  }
}
