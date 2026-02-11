package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.AppUserEntity;
import com.akdemya.domain.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public AppUser toDomain(AppUserEntity entity) {
    return new AppUser(
        entity.getId(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getRole(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getOccupation()
    );
  }

  public AppUserEntity toEntity(AppUser domain) {
    AppUserEntity entity = new AppUserEntity(domain.getEmail(), domain.getPasswordHash());
    entity.setId(domain.getId());
    entity.setRole(domain.getRole());
    entity.setFirstName(domain.getFirstName());
    entity.setLastName(domain.getLastName());
    entity.setOccupation(domain.getOccupation());
    return entity;
  }
}
