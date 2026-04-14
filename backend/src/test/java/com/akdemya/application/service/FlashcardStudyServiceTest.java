package com.akdemya.application.service;

import com.akdemya.application.config.FlashcardSchedulerProperties;
import com.akdemya.domain.model.Flashcard;
import com.akdemya.domain.model.FlashcardReview;
import com.akdemya.domain.model.ReviewState;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.UserSettings;
import com.akdemya.domain.model.Visibility;
import com.akdemya.domain.port.in.FlashcardStudyUseCase;
import com.akdemya.domain.port.out.FlashcardRepository;
import com.akdemya.domain.port.out.FlashcardReviewRepository;
import com.akdemya.domain.port.out.SubjectRepository;
import com.akdemya.domain.port.out.SyllabusRepository;
import com.akdemya.domain.port.out.UnitRepository;
import com.akdemya.domain.port.out.UserSettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FlashcardStudyServiceTest {

  private final UUID userId = UUID.randomUUID();
  private final UUID unitId = UUID.randomUUID();
  private final LocalDateTime now = LocalDateTime.of(2026, 2, 24, 10, 0);
  private final FlashcardSchedulerProperties schedulerProperties = new FlashcardSchedulerProperties();
  private final InMemoryUserSettingsRepo settingsRepo = new InMemoryUserSettingsRepo();
  private final StubUnitRepository stubUnitRepo = new StubUnitRepository();
  private final StubSubjectRepository stubSubjectRepo = new StubSubjectRepository();
  private final StubSyllabusRepository stubSyllabusRepo = new StubSyllabusRepository();

  @Test
  void studyQueueCountsDueLearningAndNew() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard learningCard = flashcard("learning", unitId);
    Flashcard reviewCard = flashcard("review", unitId);
    Flashcard newCard = flashcard("new", unitId);
    flashcardRepo.save(learningCard);
    flashcardRepo.save(reviewCard);
    flashcardRepo.save(newCard);

    reviewRepo.save(review(learningCard.getId(), now.minusMinutes(5), ReviewState.LEARNING));
    reviewRepo.save(review(reviewCard.getId(), now.minusMinutes(10), ReviewState.REVIEW));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 5, now));

    assertEquals(1, response.learningCount());
    assertEquals(1, response.dueCount());
    assertEquals(1, response.newCount());
  }

  @Test
  void studyNextPrioritizesDue() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard dueCard = flashcard("due", unitId);
    Flashcard newCard = flashcard("new", unitId);
    flashcardRepo.save(dueCard);
    flashcardRepo.save(newCard);

    reviewRepo.save(review(dueCard.getId(), now.minusMinutes(5)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var next = service.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(userId, unitId, now));

    assertNotNull(next);
    assertEquals(dueCard.getId(), next.flashcardId());
  }

  @Test
  void studyQueueReturnsZerosForUserWithNoCards() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, null, 5, now));

    assertEquals(0, response.newCount());
    assertEquals(0, response.dueCount());
    assertEquals(0, response.learningCount());
  }

  @Test
  void studyQueueOnlyNewCards() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    flashcardRepo.save(flashcard("card1", unitId));
    flashcardRepo.save(flashcard("card2", unitId));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, null, 5, now));

    assertEquals(2, response.newCount());
    assertEquals(0, response.dueCount());
    assertEquals(0, response.learningCount());
  }

  @Test
  void studyQueueOnlyLearningDue() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard card = flashcard("card1", unitId);
    flashcardRepo.save(card);
    reviewRepo.save(review(card.getId(), now.minusMinutes(5), ReviewState.LEARNING));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, null, 5, now));

    assertEquals(0, response.newCount());
    assertEquals(0, response.dueCount());
    assertEquals(1, response.learningCount());
  }

  @Test
  void studyQueueGlobalAcrossUnits() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    UUID unitId2 = UUID.randomUUID();
    flashcardRepo.save(flashcard("card-unit1", unitId));
    flashcardRepo.save(flashcard("card-unit2", unitId2));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, null, 5, now));

    assertEquals(2, response.newCount());
    assertEquals(0, response.dueCount());
    assertEquals(0, response.learningCount());
  }

  @Test
  void studyQueueFutureCardsNotCounted() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard learningCard = flashcard("learning-future", unitId);
    Flashcard reviewCard = flashcard("review-future", unitId);
    flashcardRepo.save(learningCard);
    flashcardRepo.save(reviewCard);
    reviewRepo.save(review(learningCard.getId(), now.plusMinutes(30), ReviewState.LEARNING));
    reviewRepo.save(review(reviewCard.getId(), now.plusDays(1), ReviewState.REVIEW));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, null, 5, now));

    assertEquals(0, response.newCount());
    assertEquals(0, response.dueCount());
    assertEquals(0, response.learningCount());
  }

  @Test
  void dashboardCountsAreCoherent() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard today = flashcard("today", unitId);
    Flashcard in2 = flashcard("in2", unitId);
    Flashcard in5 = flashcard("in5", unitId);
    Flashcard in10 = flashcard("in10", unitId);
    Flashcard newCard = flashcard("new", unitId);
    flashcardRepo.save(today);
    flashcardRepo.save(in2);
    flashcardRepo.save(in5);
    flashcardRepo.save(in10);
    flashcardRepo.save(newCard);

    reviewRepo.save(review(today.getId(), now.minusHours(1)));
    reviewRepo.save(review(in2.getId(), now.plusDays(2)));
    reviewRepo.save(review(in5.getId(), now.plusDays(5)));
    reviewRepo.save(review(in10.getId(), now.plusDays(10)));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var dashboard = service.getDashboard(new FlashcardStudyUseCase.DashboardCommand(userId, unitId, now));

    assertEquals(1, dashboard.dueToday());
    assertEquals(1, dashboard.dueIn1to3Days());
    assertEquals(1, dashboard.dueIn4to7Days());
    assertEquals(1, dashboard.dueIn8to30Days());
    assertEquals(1, dashboard.newCards());
    assertEquals(4, dashboard.totalDue());
  }

  private Flashcard flashcard(String front, UUID unitId) {
    return new Flashcard(UUID.randomUUID(), unitId, front, "back", now.minusDays(10), now.minusDays(1));
  }

  private FlashcardReview review(UUID flashcardId, LocalDateTime dueAt) {
    return review(flashcardId, dueAt, ReviewState.LEARNING);
  }

  private FlashcardReview review(UUID flashcardId, LocalDateTime dueAt, ReviewState state) {
    return new FlashcardReview(UUID.randomUUID(), userId, flashcardId,
        state, 2.5, 1, 0, 1, 0, dueAt, now.minusDays(1), now.minusDays(10), now.minusDays(1));
  }

  static class InMemoryFlashcardRepo implements FlashcardRepository {
    private final Map<UUID, Flashcard> data = new ConcurrentHashMap<>();
    private InMemoryReviewRepo reviewRepo;

    void attach(InMemoryReviewRepo reviewRepo) {
      this.reviewRepo = reviewRepo;
    }

    @Override
    public Optional<Flashcard> findById(UUID id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Flashcard> findByIds(List<UUID> ids) {
      if (ids == null || ids.isEmpty()) return List.of();
      return ids.stream().map(data::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<Flashcard> findByUnitId(UUID unitId) {
      return data.values().stream().filter(f -> f.getUnitId().equals(unitId)).toList();
    }

    @Override
    public List<Flashcard> findNewByUserIdAndUnitId(UUID userId, UUID unitId, int limit) {
      List<Flashcard> cards = findByUnitId(unitId).stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .sorted(Comparator.comparing(Flashcard::getCreatedAt))
          .toList();
      return cards.subList(0, Math.min(limit, cards.size()));
    }

    @Override
    public long countNewByUserIdAndUnitId(UUID userId, UUID unitId) {
      return findByUnitId(unitId).stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .count();
    }

    @Override
    public long countNewByUserId(UUID userId) {
      return data.values().stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .count();
    }

    @Override
    public Flashcard save(Flashcard flashcard) {
      data.put(flashcard.getId(), flashcard);
      return flashcard;
    }

    @Override
    public void deleteById(UUID id) {
      data.remove(id);
    }

    @Override
    public java.util.List<Flashcard> findVisibleByUserId(UUID userId) {
      return data.values().stream()
          .filter(f -> f.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
              || userId.equals(f.getOwnerId()))
          .toList();
    }

    @Override
    public java.util.List<Flashcard> findVisibleByUnitIdAndUserId(UUID unitId, UUID userId) {
      return data.values().stream()
          .filter(f -> f.getUnitId().equals(unitId)
              && (f.getVisibility() == com.akdemya.domain.model.Visibility.GLOBAL
                  || userId.equals(f.getOwnerId())))
          .toList();
    }

    @Override
    public Map<UUID, Long> countNewByUserIdGroupByUnit(UUID userId) {
      Map<UUID, Long> result = new ConcurrentHashMap<>();
      data.values().stream()
          .filter(f -> reviewRepo.findByUserIdAndFlashcardId(userId, f.getId()).isEmpty())
          .forEach(f -> result.merge(f.getUnitId(), 1L, (a, b) -> a + b));
      return result;
    }
  }

  @Test
  void studyNextReturnsNullWhenNoDueAndNoNew() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var next = service.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(userId, unitId, now));

    assertNull(next);
  }

  @Test
  void studyNextReturnsNewCardWhenNoDueReviews() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    Flashcard newCard = flashcard("new", unitId);
    flashcardRepo.save(newCard);

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var next = service.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(userId, unitId, now));

    assertNotNull(next);
    assertEquals(newCard.getId(), next.flashcardId());
    assertEquals(ReviewState.NEW, next.state());
    assertNotNull(next.intervalHints());
  }

  @Test
  void studyNextThrowsOnNullCommand() {
    FlashcardStudyService service = new FlashcardStudyService(
        new InMemoryFlashcardRepo(), new InMemoryReviewRepo(new InMemoryFlashcardRepo()),
        settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);

    assertThrows(IllegalArgumentException.class, () ->
        service.getStudyNext(null));
  }

  @Test
  void studyNextThrowsOnNullUserId() {
    FlashcardStudyService service = new FlashcardStudyService(
        new InMemoryFlashcardRepo(), new InMemoryReviewRepo(new InMemoryFlashcardRepo()),
        settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);

    assertThrows(IllegalArgumentException.class, () ->
        service.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(null, unitId, now)));
  }

  @Test
  void studyNextThrowsOnNullUnitId() {
    FlashcardStudyService service = new FlashcardStudyService(
        new InMemoryFlashcardRepo(), new InMemoryReviewRepo(new InMemoryFlashcardRepo()),
        settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);

    assertThrows(IllegalArgumentException.class, () ->
        service.getStudyNext(new FlashcardStudyUseCase.StudyNextCommand(userId, null, now)));
  }

  @Test
  void studyQueueCapsNewCountByUserLimit() {
    InMemoryFlashcardRepo flashcardRepo = new InMemoryFlashcardRepo();
    InMemoryReviewRepo reviewRepo = new InMemoryReviewRepo(flashcardRepo);
    flashcardRepo.attach(reviewRepo);

    for (int i = 0; i < 5; i++) {
      flashcardRepo.save(flashcard("card" + i, unitId));
    }

    settingsRepo.save(new UserSettings(userId, 3, 100, 3));

    FlashcardStudyService service = new FlashcardStudyService(flashcardRepo, reviewRepo, settingsRepo, stubUnitRepo, stubSubjectRepo, stubSyllabusRepo, schedulerProperties);
    var response = service.getStudyQueue(new FlashcardStudyUseCase.StudyQueueCommand(userId, unitId, 5, now));

    assertEquals(3, response.newCount(), "newCount should be capped by user's newCardsLimit");
  }

  static class InMemoryUserSettingsRepo implements UserSettingsRepository {
    private final Map<UUID, UserSettings> data = new ConcurrentHashMap<>();

    @Override
    public Optional<UserSettings> findByUserId(UUID userId) {
      return Optional.ofNullable(data.get(userId));
    }

    @Override
    public UserSettings save(UserSettings settings) {
      data.put(settings.userId(), settings);
      return settings;
    }
  }

  static class InMemoryReviewRepo implements FlashcardReviewRepository {
    private final Map<UUID, FlashcardReview> data = new ConcurrentHashMap<>();
    private final InMemoryFlashcardRepo flashcardRepo;

    InMemoryReviewRepo(InMemoryFlashcardRepo flashcardRepo) {
      this.flashcardRepo = flashcardRepo;
    }

    @Override
    public Optional<FlashcardReview> findByUserIdAndFlashcardId(UUID userId, UUID flashcardId) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && r.getFlashcardId().equals(flashcardId))
          .findFirst();
    }

    @Override
    public List<FlashcardReview> findDueByUserId(UUID userId, LocalDateTime upTo, int limit) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && !r.getDueAt().isAfter(upTo))
          .sorted(Comparator.comparing(FlashcardReview::getDueAt))
          .limit(limit)
          .toList();
    }

    @Override
    public List<FlashcardReview> findDueByUserIdAndUnitId(UUID userId, UUID unitId, LocalDateTime upTo, int limit) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && !r.getDueAt().isAfter(upTo))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .sorted(Comparator.comparing(FlashcardReview::getDueAt))
          .limit(limit)
          .toList();
    }

    @Override
    public long countDueByUserIdAndUnitIdUpTo(UUID userId, UUID unitId, LocalDateTime upTo) {
      return findDueByUserIdAndUnitId(userId, unitId, upTo, Integer.MAX_VALUE).size();
    }

    @Override
    public long countDueByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, LocalDateTime upTo,
                                                    List<ReviewState> states) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId))
          .filter(r -> states.contains(r.getState()))
          .filter(r -> !r.getDueAt().isAfter(upTo))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .count();
    }

    @Override
    public long countByUserIdAndUnitIdAndStateIn(UUID userId, UUID unitId, List<ReviewState> states) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId))
          .filter(r -> states.contains(r.getState()))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .count();
    }

    @Override
    public long countDueByUserIdAndUnitIdBetween(UUID userId, UUID unitId, LocalDateTime fromExclusive,
                                                 LocalDateTime toInclusive) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId))
          .filter(r -> r.getDueAt().isAfter(fromExclusive) && !r.getDueAt().isAfter(toInclusive))
          .filter(r -> flashcardRepo.findById(r.getFlashcardId())
              .map(f -> f.getUnitId().equals(unitId)).orElse(false))
          .count();
    }

    @Override
    public long countDueByUserIdAndStateIn(UUID userId, LocalDateTime upTo, List<ReviewState> states) {
      return data.values().stream()
          .filter(r -> r.getUserId().equals(userId))
          .filter(r -> states.contains(r.getState()))
          .filter(r -> !r.getDueAt().isAfter(upTo))
          .count();
    }

    @Override
    public FlashcardReview save(FlashcardReview review) {
      data.put(review.getId(), review);
      return review;
    }

    @Override
    public Map<UUID, Long> countByUserIdAndStateInGroupByUnit(UUID userId, List<ReviewState> states) {
      Map<UUID, Long> result = new ConcurrentHashMap<>();
      data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && states.contains(r.getState()))
          .forEach(r -> flashcardRepo.findById(r.getFlashcardId())
              .ifPresent(f -> result.merge(f.getUnitId(), 1L, (a, b) -> a + b)));
      return result;
    }

    @Override
    public Map<UUID, Long> countDueByUserIdUpToGroupByUnit(UUID userId, LocalDateTime upTo) {
      Map<UUID, Long> result = new ConcurrentHashMap<>();
      data.values().stream()
          .filter(r -> r.getUserId().equals(userId) && !r.getDueAt().isAfter(upTo))
          .forEach(r -> flashcardRepo.findById(r.getFlashcardId())
              .ifPresent(f -> result.merge(f.getUnitId(), 1L, (a, b) -> a + b)));
      return result;
    }
  }

  static class StubUnitRepository implements UnitRepository {
    private final Map<UUID, Unit> data = new ConcurrentHashMap<>();

    void put(Unit unit) { data.put(unit.getId(), unit); }

    @Override public List<Unit> findAll() { return List.copyOf(data.values()); }
    @Override public List<Unit> findAllWithFlashcards() { return findAll(); }
    @Override public Optional<Unit> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
    @Override public Unit save(Unit unit) { data.put(unit.getId(), unit); return unit; }
    @Override public void deleteById(UUID id) { data.remove(id); }
    @Override public List<Unit> findBySubjectId(UUID subjectId) {
      return data.values().stream().filter(u -> u.getSubjectId().equals(subjectId)).toList();
    }
    @Override public List<Unit> findBySubjectIdWithFlashcards(UUID subjectId) { return findBySubjectId(subjectId); }
    @Override public List<Unit> findVisibleBySubjectIdAndUserId(UUID subjectId, UUID userId) { return findBySubjectId(subjectId); }
    @Override public List<Unit> findBySubjectIdAndScope(UUID subjectId, UUID userId, Visibility scope) { return findBySubjectId(subjectId); }
    @Override public List<Unit> findVisibleWithFlashcardsByUserId(UUID userId) { return List.copyOf(data.values()); }
  }

  static class StubSubjectRepository implements SubjectRepository {
    private final Map<UUID, Subject> data = new ConcurrentHashMap<>();

    void put(Subject subject) { data.put(subject.getId(), subject); }

    @Override public List<Subject> findAll() { return List.copyOf(data.values()); }
    @Override public Optional<Subject> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
    @Override public Subject save(Subject subject) { data.put(subject.getId(), subject); return subject; }
    @Override public void deleteById(UUID id) { data.remove(id); }
    @Override public List<Subject> findVisibleByUserId(UUID userId) { return findAll(); }
    @Override public List<Subject> findByScope(UUID userId, Visibility scope) { return findAll(); }
    @Override public List<Subject> findBySyllabusId(UUID syllabusId) { return findAll(); }
    @Override public List<Subject> findVisibleBySyllabusId(UUID syllabusId, UUID userId) { return findAll(); }
  }

  static class StubSyllabusRepository implements SyllabusRepository {
    private final Map<UUID, com.akdemya.domain.model.Syllabus> data = new ConcurrentHashMap<>();

    @Override public List<com.akdemya.domain.model.Syllabus> findAll() { return List.copyOf(data.values()); }
    @Override public Optional<com.akdemya.domain.model.Syllabus> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
    @Override public com.akdemya.domain.model.Syllabus save(com.akdemya.domain.model.Syllabus syllabus) { data.put(syllabus.getId(), syllabus); return syllabus; }
    @Override public void deleteById(UUID id) { data.remove(id); }
    @Override public List<com.akdemya.domain.model.Syllabus> findVisibleByUserId(UUID userId) { return findAll(); }
  }
}
