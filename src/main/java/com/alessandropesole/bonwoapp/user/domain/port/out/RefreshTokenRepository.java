package com.alessandropesole.bonwoapp.user.domain.port.out;

import com.alessandropesole.bonwoapp.user.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenId(String tokenId);
}
