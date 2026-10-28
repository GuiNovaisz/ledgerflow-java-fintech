package com.ledgerflow.security;

import com.ledgerflow.auth.AuthDtos.TokenResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenResponse issue(UUID userId) {
        Instant now = Instant.now();
        Instant expires = now.plus(properties.getTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.getIssuer())
                .issuedAt(now).expiresAt(expires).subject(userId.toString())
                .audience(List.of("ledgerflow-api")).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.getTtl().toSeconds());
    }
}
