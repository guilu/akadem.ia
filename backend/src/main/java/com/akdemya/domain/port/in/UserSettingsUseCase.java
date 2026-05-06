package com.akdemya.domain.port.in;

import java.util.UUID;

public interface UserSettingsUseCase {

    GetSettingsResponse getSettings(UUID userId);

    GetSettingsResponse updateSettings(UUID userId, UpdateSettingsCommand command);

    record GetSettingsResponse(int newCardsLimit, int reviewCardsLimit, int penaltyRatio) {}

    record UpdateSettingsCommand(int newCardsLimit, int reviewCardsLimit, int penaltyRatio) {}
}
