package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.UnitEntity;
import com.akdemya.domain.model.Unit;
import org.springframework.stereotype.Component;

@Component
public class UnitMapper {
  public Unit toDomain(UnitEntity entity) {
    return new Unit(entity.getId(), entity.getSubjectId(), entity.getName(), entity.getOrderIndex());
  }

  public UnitEntity toEntity(Unit domain) {
    UnitEntity entity = new UnitEntity(null, domain.getName(), domain.getOrderIndex());
    entity.setId(domain.getId());
    return entity;
  }
}
