package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.UserSettingsEntity;
import com.akdemya.domain.model.UserSettings;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserSettingsMapper {

    public UserSettings toDomain(UserSettingsEntity e) {
        return new UserSettings(e.getUserId(), e.getNewCardsLimit(), e.getReviewCardsLimit(), e.getPenaltyRatio());
    }

    public UserSettingsEntity toEntity(UserSettings d, UserSettingsEntity existing) {
        UserSettingsEntity e = existing != null ? existing : new UserSettingsEntity();
        e.setUserId(d.userId());
        e.setNewCardsLimit(d.newCardsLimit());
        e.setReviewCardsLimit(d.reviewCardsLimit());
        e.setPenaltyRatio(d.penaltyRatio());
        e.setUpdatedAt(Instant.now());
        if (existing == null) {
            e.setCreatedAt(Instant.now());
        }
        return e;
    }
}
