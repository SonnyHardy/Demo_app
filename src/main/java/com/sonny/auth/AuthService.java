package com.sonny.auth;

import com.sonny.auth.dto.AuthResponse;
import com.sonny.auth.dto.LoginRequest;
import com.sonny.auth.dto.RegisterRequest;
import com.sonny.config.JwtProperties;
import com.sonny.exception.EmailAlreadyExistsException;
import com.sonny.user.User;
import com.sonny.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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
        return new AuthResponse(generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        return new AuthResponse(generateToken(authentication.getName()));
    }

    public void logout(String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        try {
            Jwt jwt = jwtDecoder.decode(token);
            tokenBlacklist.revoke(jwt.getId());
        } catch (JwtException ignored) {
            // Token already invalid — nothing to revoke
        }
    }

    private String generateToken(String email) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtProperties.expirationMs()))
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
