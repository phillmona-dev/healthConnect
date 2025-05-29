package com.medco.HealthConnectProvider.services.impl.token;

import com.medco.HealthConnectProvider.entity.token.RefreshToken;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ExpiredJwtException;
import com.medco.HealthConnectProvider.repository.token.RefreshTokenRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.token.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository tokenRepository;

    @Value("${medcoanalytics.app.refreshExpirationMs}")
    private Long expiryTime;

    public TokenServiceImpl(UserRepository userRepository, RefreshTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public RefreshToken createRefreshToken(String email) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User with the provided email not found")))
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(expiryTime))
                .build();

        return tokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        if(refreshToken.getExpiryDate().compareTo(Instant.now()) < 0){
            tokenRepository.delete(refreshToken);
            throw new ExpiredJwtException("Jwt Token is expired. Please try to Authenticate Again..!");
        }
        return refreshToken;
    }
}
