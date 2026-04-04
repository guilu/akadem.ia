package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.QuestionEntity;
import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import com.akdemya.adapter.outbound.persistence.mapper.QuestionMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataQuestionRepository;
import com.akdemya.domain.model.Question;
import com.akdemya.domain.model.Visibility;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuestionPersistenceAdapterTest {

  private final SpringDataQuestionRepository repository = mock(SpringDataQuestionRepository.class);
  private final QuestionMapper mapper = new QuestionMapper();
  // No EntityManager needed for delegation tests (save uses entityManager internally, skip that path)
  // We test all methods that do NOT rely on EntityManager.getReference
  private final QuestionPersistenceAdapter adapter = new QuestionPersistenceAdapter(repository, mapper);

  private QuestionEntity globalEntity(UUID unitId, String text) {
    QuestionEntity entity = new QuestionEntity();
    entity.setId(UUID.randomUUID());
    entity.setText(text);
    entity.setDifficulty("MEDIUM");
    entity.setExplanation("exp");
    entity.setVisibility("GLOBAL");
    UnitEntity unit = new UnitEntity();
    unit.setId(unitId);
    entity.setUnit(unit);
    return entity;
  }

  private QuestionEntity privateEntity(UUID unitId, String text, UUID ownerId) {
    QuestionEntity entity = globalEntity(unitId, text);
    entity.setVisibility("PRIVATE");
    entity.setOwnerId(ownerId);
    return entity;
  }

  @Test
  void findByUnitIdReturnsMappedQuestions() {
    UUID unitId = UUID.randomUUID();
    QuestionEntity entity = globalEntity(unitId, "Q1?");
    when(repository.findByUnit_Id(unitId)).thenReturn(List.of(entity));

    List<Question> result = adapter.findByUnitId(unitId);

    assertEquals(1, result.size());
    assertEquals("Q1?", result.get(0).getText());
    assertEquals(Visibility.GLOBAL, result.get(0).getVisibility());
  }

  @Test
  void findByIdReturnsMappedQuestion() {
    UUID id = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    QuestionEntity entity = globalEntity(unitId, "Q?");
    entity.setId(id);
    when(repository.findById(id)).thenReturn(Optional.of(entity));

    Optional<Question> result = adapter.findById(id);

    assertTrue(result.isPresent());
    assertEquals(id, result.get().getId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    Optional<Question> result = adapter.findById(id);

    assertTrue(result.isEmpty());
  }

  @Test
  void deleteByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    adapter.deleteById(id);
    verify(repository).deleteById(id);
  }

  @Test
  void countByUnitIdDelegatesToRepository() {
    UUID unitId = UUID.randomUUID();
    when(repository.countByUnit_Id(unitId)).thenReturn(7L);

    long count = adapter.countByUnitId(unitId);

    assertEquals(7L, count);
  }

  @Test
  void countByUnitIdAndDifficultyDelegatesToRepository() {
    UUID unitId = UUID.randomUUID();
    when(repository.countByUnit_IdAndDifficulty(unitId, "EASY")).thenReturn(3L);

    long count = adapter.countByUnitIdAndDifficulty(unitId, "EASY");

    assertEquals(3L, count);
  }

  @Test
  void findVisibleByUnitIdAndUserIdReturnsMixedVisibility() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    QuestionEntity global = globalEntity(unitId, "Global Q");
    QuestionEntity priv = privateEntity(unitId, "Private Q", userId);
    when(repository.findVisibleByUnitIdAndUserId(unitId, userId)).thenReturn(List.of(global, priv));

    List<Question> result = adapter.findVisibleByUnitIdAndUserId(unitId, userId);

    assertEquals(2, result.size());
    assertTrue(result.stream().anyMatch(q -> q.getVisibility() == Visibility.GLOBAL));
    assertTrue(result.stream().anyMatch(q -> q.getVisibility() == Visibility.PRIVATE));
  }

  @Test
  void findPageByUnitIdAndScopeNullReturnsAll() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    QuestionEntity entity = globalEntity(unitId, "Q");
    Page<QuestionEntity> page = new PageImpl<>(List.of(entity));
    when(repository.findAllByUnitIdAndScope(eq(unitId), eq(userId), any())).thenReturn(page);

    Page<Question> result = adapter.findPageByUnitIdAndScope(unitId, userId, null, 0, 10);

    assertEquals(1, result.getTotalElements());
    verify(repository).findAllByUnitIdAndScope(eq(unitId), eq(userId), any());
  }

  @Test
  void findPageByUnitIdAndScopeGlobalReturnsOnlyGlobal() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    QuestionEntity entity = globalEntity(unitId, "GQ");
    Page<QuestionEntity> page = new PageImpl<>(List.of(entity));
    when(repository.findGlobalByUnitId(eq(unitId), any())).thenReturn(page);

    Page<Question> result = adapter.findPageByUnitIdAndScope(unitId, userId, Visibility.GLOBAL, 0, 10);

    assertEquals(1, result.getTotalElements());
    verify(repository).findGlobalByUnitId(eq(unitId), any());
  }

  @Test
  void findPageByUnitIdAndScopePrivateReturnsOnlyOwned() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    QuestionEntity entity = privateEntity(unitId, "PQ", userId);
    Page<QuestionEntity> page = new PageImpl<>(List.of(entity));
    when(repository.findPrivateByUnitIdAndOwner(eq(unitId), eq(userId), any())).thenReturn(page);

    Page<Question> result = adapter.findPageByUnitIdAndScope(unitId, userId, Visibility.PRIVATE, 0, 10);

    assertEquals(1, result.getTotalElements());
    verify(repository).findPrivateByUnitIdAndOwner(eq(unitId), eq(userId), any());
  }

  @Test
  void findAllReturnsMappedQuestions() {
    UUID unitId = UUID.randomUUID();
    QuestionEntity entity = globalEntity(unitId, "Q2");
    when(repository.findAll()).thenReturn(List.of(entity));

    List<Question> result = adapter.findAll();

    assertEquals(1, result.size());
  }
}
