package com.medco.HealthConnectProvider.services.token;

import com.medco.HealthConnectProvider.entity.token.RefreshToken;

import java.util.Optional;

public interface TokenService {
    RefreshToken createRefreshToken(String email);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyExpiration(RefreshToken refreshToken);
}

