package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SubjectEntity;
import com.akdemya.domain.model.Subject;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {
  public Subject toDomain(SubjectEntity entity) {
    return new Subject(entity.getId(), entity.getName(), entity.getDescription());
  }

  public SubjectEntity toEntity(Subject domain) {
    SubjectEntity entity = new SubjectEntity(domain.getName(), domain.getDescription());
    entity.setId(domain.getId());
    return entity;
  }
}
