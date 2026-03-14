package com.akdemya.domain.port.out;

import com.akdemya.domain.model.UserSettings;

import java.util.Optional;
import java.util.UUID;

public interface UserSettingsRepository {

    Optional<UserSettings> findByUserId(UUID userId);

    UserSettings save(UserSettings settings);
}
