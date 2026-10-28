package com.ledgerflow.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {
    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        if (properties.getTtl() == null || properties.getTtl().isZero() || properties.getTtl().isNegative()) {
            throw new IllegalStateException("ledgerflow.jwt.ttl must be a positive duration");
        }
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException("ledgerflow.jwt.issuer must not be blank");
        }
        if (properties.getSecret() == null || properties.getSecret().isBlank()) {
            throw new IllegalStateException("ledgerflow.jwt.secret must be configured as Base64");
        }
        final byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.getSecret());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("ledgerflow.jwt.secret must be valid Base64", ex);
        }
        if (key.length < 32) throw new IllegalStateException("ledgerflow.jwt.secret must decode to at least 32 bytes");
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        var claimConverter = org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter.withDefaults(java.util.Map.of());
        decoder.setClaimSetConverter(claims -> {
            var converted = claimConverter.convert(claims);
            // Spring's default converter synthesizes iat from exp; require the actual signed claim.
            if (!claims.containsKey("iat")) converted.remove("iat");
            return converted;
        });
        OAuth2TokenValidator<Jwt> defaults = new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new org.springframework.security.oauth2.jwt.JwtTimestampValidator(java.time.Duration.ZERO),
                new org.springframework.security.oauth2.jwt.JwtIssuerValidator(properties.getIssuer()));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains("ledgerflow-api")
                ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Invalid audience", null));
        OAuth2TokenValidator<Jwt> subject = jwt -> {
            try {
                String value = jwt.getSubject();
                UUID parsed = UUID.fromString(value);
                if (!parsed.toString().equals(value)) throw new IllegalArgumentException("Non-canonical UUID");
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) {
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Invalid subject", null));
            }
        };
        OAuth2TokenValidator<Jwt> requiredTimes = jwt -> jwt.getExpiresAt() != null && jwt.getIssuedAt() != null
                ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Missing token timestamps", null));
        decoder.setJwtValidator(jwt -> {
            var result = defaults.validate(jwt);
            if (result.hasErrors()) return result;
            result = requiredTimes.validate(jwt);
            if (result.hasErrors()) return result;
            result = audience.validate(jwt);
            return result.hasErrors() ? result : subject.validate(jwt);
        });
        return decoder;
    }
}
