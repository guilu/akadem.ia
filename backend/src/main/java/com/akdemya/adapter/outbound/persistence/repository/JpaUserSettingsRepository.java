package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserSettingsRepository extends JpaRepository<UserSettingsEntity, UUID> {

    Optional<UserSettingsEntity> findByUserId(UUID userId);
}
