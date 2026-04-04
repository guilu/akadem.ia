package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.domain.model.Subject;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {
  public Subject toDomain(SubjectEntity entity) {
    Visibility visibility = entity.getVisibility() != null
        ? Visibility.valueOf(entity.getVisibility())
        : Visibility.GLOBAL;
    return new Subject(entity.getId(), entity.getName(), entity.getDescription(),
        visibility, entity.getOwnerId());
  }

  public SubjectEntity toEntity(Subject domain) {
    SubjectEntity entity = new SubjectEntity(domain.getName(), domain.getDescription());
    entity.setId(domain.getId());
    entity.setVisibility(domain.getVisibility() != null ? domain.getVisibility().name() : Visibility.GLOBAL.name());
    entity.setOwnerId(domain.getOwnerId());
    return entity;
  }
}
