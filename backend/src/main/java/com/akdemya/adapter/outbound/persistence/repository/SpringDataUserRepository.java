package com.akdemya.adapter.outbound.persistence.repository;

import com.akdemya.adapter.outbound.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<AppUserEntity, UUID> {
    Optional<AppUserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
