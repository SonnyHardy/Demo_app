package com.sonny.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
        @NotBlank String secret,
        @Min(60000) long expirationMs,           // Minimum 1 minute
        @Min(60000) long refreshTokenExpirationMs  // Minimum 1 minute
) {}
