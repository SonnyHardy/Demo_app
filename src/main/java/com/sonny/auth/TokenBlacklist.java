package com.sonny.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of revoked JWT IDs (jti claims).
 * Sufficient for a single-node demo; use Redis or a DB table in production.
 * todo: use Redis or a DB table in production
 */
@Component
public class TokenBlacklist {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void revokeToken(String tokenId, Instant expiration) {
        blacklist.put(tokenId, expiration);
    }

    public boolean isRevoked(String tokenId) {
        return blacklist.containsKey(tokenId);
    }

    // todo: schedule periodic cleanup of expired tokens to prevent memory bloat
    public void cleanupExpiredTokens() {
        blacklist.entrySet().removeIf(entry -> Instant.now().isAfter(entry.getValue()));
    }
}
