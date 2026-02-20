package com.sonny.auth;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of revoked JWT IDs (jti claims).
 * Sufficient for a single-node demo; use Redis or a DB table in production.
 */
@Component
public class TokenBlacklist {

    private final Set<String> revokedJtis = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void revoke(String jti) {
        revokedJtis.add(jti);
    }

    public boolean isRevoked(String jti) {
        return revokedJtis.contains(jti);
    }
}
