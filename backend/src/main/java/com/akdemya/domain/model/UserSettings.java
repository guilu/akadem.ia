package com.akdemya.domain.model;

import java.util.UUID;

public record UserSettings(
    UUID userId,
    Integer newCardsLimit,
    Integer reviewCardsLimit
) {}
