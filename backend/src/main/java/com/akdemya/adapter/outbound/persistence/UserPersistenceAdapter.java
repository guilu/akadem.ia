package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.entity.AppUserEntity;
import com.akdemya.adapter.outbound.persistence.mapper.UserMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataUserRepository;
import com.akdemya.domain.model.AppUser;
import com.akdemya.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepository {
  private final SpringDataUserRepository repository;
  private final UserMapper mapper;

  public UserPersistenceAdapter(SpringDataUserRepository repository, UserMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<AppUser> findByEmail(String email) {
    return repository.findByEmail(email).map(mapper::toDomain);
  }

  @Override
  public AppUser save(AppUser user) {
    AppUserEntity entity = mapper.toEntity(user);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<AppUser> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public boolean existsByEmail(String email) {
    return repository.existsByEmail(email);
  }
}
