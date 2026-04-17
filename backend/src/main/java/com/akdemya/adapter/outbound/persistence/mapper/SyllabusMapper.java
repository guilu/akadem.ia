package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.SyllabusEntity;
import com.akdemya.domain.model.Syllabus;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

@Component
public class SyllabusMapper {
  public Syllabus toDomain(SyllabusEntity entity) {
    Visibility visibility = entity.getVisibility() != null
        ? Visibility.valueOf(entity.getVisibility())
        : Visibility.GLOBAL;
    return new Syllabus(entity.getId(), entity.getName(), entity.getDescription(),
        visibility, entity.getOwnerId());
  }

  public SyllabusEntity toEntity(Syllabus domain) {
    SyllabusEntity entity = new SyllabusEntity(domain.getName(), domain.getDescription());
    entity.setId(domain.getId());
    entity.setVisibility(domain.getVisibility() != null ? domain.getVisibility().name() : Visibility.GLOBAL.name());
    entity.setOwnerId(domain.getOwnerId());
    return entity;
  }
}
