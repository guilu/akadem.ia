package com.akdemya.application.service;

import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.in.UserProfileUseCase;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class UserProfileService implements UserProfileUseCase {

  private final UserRepository userRepository;

  public UserProfileService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public GetProfileResponse getProfile(UUID userId) {
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    return toResponse(user);
  }

  @Override
  public GetProfileResponse updateProfile(UUID userId, UpdateProfileCommand command) {
    AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

    validateName(command.firstName(), "firstName");
    validateName(command.lastName(), "lastName");

    AppUser updated = new AppUser(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getRole(),
        command.firstName(),
        command.lastName(),
        user.getOccupation()
    );

    AppUser saved = userRepository.save(updated);
    return toResponse(saved);
  }

  private void validateName(String value, String field) {
    if (value != null && value.length() > 100) {
      throw new IllegalArgumentException(field + "_too_long");
    }
  }

  private GetProfileResponse toResponse(AppUser user) {
    return new GetProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName()
    );
  }
}
