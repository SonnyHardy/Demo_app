package com.sonny.auth;

import com.sonny.config.JwtProperties;
import com.sonny.exception.InvalidRefreshTokenException;
import com.sonny.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /**
     * Creates a new refresh token for the user, replacing any existing one.
     * Enforces a maximum of one active refresh token per user.
     */
    public RefreshToken create(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs()))
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Validates and rotates a refresh token (single-use enforcement).
     * The presented token is deleted and a new one is issued in its place.
     * If the token is not found (already used or never existed), the caller
     * must re-authenticate — this limits the impact of token theft.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshToken rotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found or already used"));

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(existing);
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        // Delete the old token before issuing a new one (rotation)
        return create(existing.getUser());
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
