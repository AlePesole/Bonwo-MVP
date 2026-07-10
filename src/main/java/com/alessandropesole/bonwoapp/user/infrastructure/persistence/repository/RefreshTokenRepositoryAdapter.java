package com.alessandropesole.bonwoapp.user.infrastructure.persistence.repository;

import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;
import com.alessandropesole.bonwoapp.user.domain.port.out.RefreshTokenRepository;
import com.alessandropesole.bonwoapp.user.infrastructure.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    @Override
    public RefreshToken save(RefreshToken token) {
        return RefreshTokenMapper.toDomain(jpa.save(RefreshTokenMapper.toEntity(token)));
    }

    @Override
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        return jpa.findByTokenId(tokenId).map(RefreshTokenMapper::toDomain);
    }
}
