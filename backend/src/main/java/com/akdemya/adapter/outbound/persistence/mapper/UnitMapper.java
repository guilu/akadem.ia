package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import com.akdemya.domain.model.Unit;
import com.akdemya.domain.model.Visibility;
import org.springframework.stereotype.Component;

@Component
public class UnitMapper {
  public Unit toDomain(UnitEntity entity) {
    Visibility visibility = entity.getVisibility() != null
        ? Visibility.valueOf(entity.getVisibility())
        : Visibility.GLOBAL;
    return new Unit(entity.getId(), entity.getSubjectId(), entity.getName(),
        entity.getDescription(), entity.getOrderIndex(), visibility, entity.getOwnerId());
  }

  public UnitEntity toEntity(Unit domain) {
    UnitEntity entity = new UnitEntity(null, domain.getName(), domain.getDescription(), domain.getOrderIndex());
    entity.setId(domain.getId());
    entity.setVisibility(domain.getVisibility() != null ? domain.getVisibility().name() : Visibility.GLOBAL.name());
    entity.setOwnerId(domain.getOwnerId());
    return entity;
  }
}
