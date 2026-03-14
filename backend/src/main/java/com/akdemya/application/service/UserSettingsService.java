package com.akdemya.application.service;

import com.akdemya.domain.model.StudySettingsDefaults;
import com.akdemya.domain.model.UserSettings;
import com.akdemya.domain.port.in.UserSettingsUseCase;
import com.akdemya.domain.port.out.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserSettingsService implements UserSettingsUseCase {

    private final UserSettingsRepository settingsRepo;

    public UserSettingsService(UserSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public GetSettingsResponse getSettings(UUID userId) {
        return settingsRepo.findByUserId(userId)
            .map(s -> new GetSettingsResponse(s.newCardsLimit(), s.reviewCardsLimit()))
            .orElseGet(() -> new GetSettingsResponse(
                StudySettingsDefaults.DEFAULT_NEW_LIMIT,
                StudySettingsDefaults.DEFAULT_REVIEW_LIMIT
            ));
    }

    @Override
    public GetSettingsResponse updateSettings(UUID userId, UpdateSettingsCommand command) {
        UserSettings existing = settingsRepo.findByUserId(userId)
            .orElse(new UserSettings(userId,
                StudySettingsDefaults.DEFAULT_NEW_LIMIT,
                StudySettingsDefaults.DEFAULT_REVIEW_LIMIT));

        UserSettings updated = new UserSettings(
            existing.userId(),
            command.newCardsLimit(),
            command.reviewCardsLimit()
        );
        UserSettings saved = settingsRepo.save(updated);
        return new GetSettingsResponse(saved.newCardsLimit(), saved.reviewCardsLimit());
    }
}
