package com.akdemya.adapter.outbound.persistence;

import com.akdemya.adapter.outbound.persistence.mapper.RefreshTokenMapper;
import com.akdemya.adapter.outbound.persistence.repository.SpringDataRefreshTokenRepository;
import com.akdemya.domain.model.RefreshToken;
import com.akdemya.domain.port.out.RefreshTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository repository;
    private final RefreshTokenMapper mapper;

    public RefreshTokenPersistenceAdapter(SpringDataRefreshTokenRepository repository,
                                          RefreshTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(RefreshToken token) {
        repository.save(mapper.toEntity(token));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String hash) {
        return repository.findByTokenHash(hash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeByTokenHash(String hash) {
        repository.revokeByTokenHash(hash);
    }

    @Override
    @Transactional
    public void deleteExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }
}
