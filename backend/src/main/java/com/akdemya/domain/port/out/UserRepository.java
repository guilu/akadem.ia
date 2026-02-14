package com.akdemya.domain.port.out;

import com.akdemya.domain.model.AppUser;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
  Optional<AppUser> findByEmail(String email);

  AppUser save(AppUser user);

  Optional<AppUser> findById(UUID id);

  boolean existsByEmail(String email);

  java.util.List<AppUser> findAll();

  void deleteById(UUID id);
}
