package com.akdemya.adapter.outbound.persistence.mapper;

import com.akdemya.adapter.outbound.persistence.entity.RefreshTokenEntity;
import com.akdemya.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
            entity.getId(),
            entity.getUserId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.isRevoked()
        );
    }

    public RefreshTokenEntity toEntity(RefreshToken domain) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        if (domain.id() != null) {
            entity.setId(domain.id());
        }
        entity.setUserId(domain.userId());
        entity.setTokenHash(domain.tokenHash());
        entity.setExpiresAt(domain.expiresAt());
        entity.setRevoked(domain.revoked());
        return entity;
    }
}
