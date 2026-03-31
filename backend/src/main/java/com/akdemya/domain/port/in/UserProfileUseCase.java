package com.akdemya.domain.port.in;

import java.util.UUID;

public interface UserProfileUseCase {

    GetProfileResponse getProfile(UUID userId);

    GetProfileResponse updateProfile(UUID userId, UpdateProfileCommand command);

    record GetProfileResponse(UUID id, String email, String firstName, String lastName) {}

    record UpdateProfileCommand(String firstName, String lastName) {}
}
