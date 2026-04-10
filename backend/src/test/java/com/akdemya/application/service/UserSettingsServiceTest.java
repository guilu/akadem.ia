package com.akdemya.application.service;

import com.akdemya.domain.model.StudySettingsDefaults;
import com.akdemya.domain.model.UserSettings;
import com.akdemya.domain.port.in.UserSettingsUseCase;
import com.akdemya.domain.port.out.UserSettingsRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class UserSettingsServiceTest {

    private final UUID userId = UUID.randomUUID();

    private UserSettingsService service(UserSettingsRepository repo) {
        return new UserSettingsService(repo);
    }

    @Test
    void returnsDefaultsWhenNoSettingsExist() {
        UserSettingsService svc = service(new InMemoryUserSettingsRepo());

        var result = svc.getSettings(userId);

        assertEquals(StudySettingsDefaults.DEFAULT_NEW_LIMIT, result.newCardsLimit());
        assertEquals(StudySettingsDefaults.DEFAULT_REVIEW_LIMIT, result.reviewCardsLimit());
        assertEquals(StudySettingsDefaults.DEFAULT_PENALTY_RATIO, result.penaltyRatio());
    }

    @Test
    void returnsPersistedSettingsWhenPresent() {
        InMemoryUserSettingsRepo repo = new InMemoryUserSettingsRepo();
        repo.save(new UserSettings(userId, 30, 150, 3));
        UserSettingsService svc = service(repo);

        var result = svc.getSettings(userId);

        assertEquals(30, result.newCardsLimit());
        assertEquals(150, result.reviewCardsLimit());
    }

    @Test
    void updateSettingsCreatesRowWhenAbsent() {
        InMemoryUserSettingsRepo repo = new InMemoryUserSettingsRepo();
        UserSettingsService svc = service(repo);

        var result = svc.updateSettings(userId,
            new UserSettingsUseCase.UpdateSettingsCommand(25, 80, 3));

        assertEquals(25, result.newCardsLimit());
        assertEquals(80, result.reviewCardsLimit());
        assertTrue(repo.findByUserId(userId).isPresent());
    }

    @Test
    void updateSettingsOverwritesExistingRow() {
        InMemoryUserSettingsRepo repo = new InMemoryUserSettingsRepo();
        repo.save(new UserSettings(userId, 10, 50, 3));
        UserSettingsService svc = service(repo);

        var result = svc.updateSettings(userId,
            new UserSettingsUseCase.UpdateSettingsCommand(40, 200, 3));

        assertEquals(40, result.newCardsLimit());
        assertEquals(200, result.reviewCardsLimit());
    }

    @Test
    void getSettings_returnsDefaultPenaltyRatioWhenNoSettingsExist() {
        UserSettingsService svc = service(new InMemoryUserSettingsRepo());

        var result = svc.getSettings(userId);

        assertEquals(3, result.penaltyRatio());
    }

    @Test
    void updateSettings_persistsPenaltyRatio5() {
        InMemoryUserSettingsRepo repo = new InMemoryUserSettingsRepo();
        UserSettingsService svc = service(repo);

        var result = svc.updateSettings(userId,
            new UserSettingsUseCase.UpdateSettingsCommand(20, 100, 5));

        assertEquals(5, result.penaltyRatio());
        assertEquals(5, repo.findByUserId(userId).get().penaltyRatio());
    }

    @Test
    void updateSettings_throwsWhenPenaltyRatioIsZero() {
        UserSettingsService svc = service(new InMemoryUserSettingsRepo());

        assertThrows(IllegalArgumentException.class, () ->
            svc.updateSettings(userId,
                new UserSettingsUseCase.UpdateSettingsCommand(20, 100, 0)));
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
}
