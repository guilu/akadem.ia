package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.UserSettingsMapper;
import com.akdemya.adapter.outbound.persistence.repository.JpaUserSettingsRepository;
import com.akdemya.domain.model.UserSettings;
import com.akdemya.domain.port.out.UserSettingsRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserSettingsPersistenceAdapter implements UserSettingsRepository {

    private final JpaUserSettingsRepository jpa;
    private final UserSettingsMapper mapper;

    public UserSettingsPersistenceAdapter(JpaUserSettingsRepository jpa, UserSettingsMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserSettings> findByUserId(UUID userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public UserSettings save(UserSettings settings) {
        var existing = jpa.findByUserId(settings.userId()).orElse(null);
        var entity = mapper.toEntity(settings, existing);
        return mapper.toDomain(jpa.save(entity));
    }
}
