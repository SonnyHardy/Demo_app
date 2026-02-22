package com.sonny.auth;

import com.sonny.auth.dto.AuthResponse;
import com.sonny.auth.dto.LoginRequest;
import com.sonny.auth.dto.RegisterRequest;
import com.sonny.config.JwtProperties;
import com.sonny.exception.EmailAlreadyExistsException;
import com.sonny.exception.InvalidRefreshTokenException;
import com.sonny.user.User;
import com.sonny.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final TokenBlacklist tokenBlacklist;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new RuntimeException("Authentication failed for user: " + request.email());
        }
        return buildResponse(user);
    }

    @Transactional
    public void logout(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        try {
            Jwt jwt = jwtDecoder.decode(token);
            tokenBlacklist.revokeToken(jwt.getId(), jwt.getExpiresAt());
            // Also invalidate the user's refresh token so it cannot be used after logout
            userRepository.findByEmail(jwt.getSubject())
                    .ifPresent(refreshTokenService::deleteByUser);
        } catch (JwtException ignored) {
            // Token already invalid — nothing to revoke
        }
    }

    /**
     * Exchanges a valid refresh token for a new access token + rotated refresh token.
     * The presented refresh token is invalidated immediately (single-use).
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken newRefreshToken = refreshTokenService.rotate(refreshTokenValue);
        User user = newRefreshToken.getUser();
        return new AuthResponse(
                generateAccessToken(user.getEmail(), user.getAuthorities()),
                newRefreshToken.getToken()
        );
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                generateAccessToken(user.getEmail(), user.getAuthorities()),
                refreshTokenService.create(user).getToken()
        );
    }

    private String generateAccessToken(String email, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("authorities", authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(authority -> !authority.equals("FACTOR_PASSWORD"))
                        .collect(Collectors.toSet()))
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtProperties.expirationMs()))
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
