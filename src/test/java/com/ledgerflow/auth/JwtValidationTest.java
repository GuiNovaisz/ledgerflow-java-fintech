package com.ledgerflow.security;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtValidationTest {
    private final byte[] keyBytes = new byte[32];
    private SecretKey key;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        new java.security.SecureRandom().nextBytes(keyBytes);
        key = new SecretKeySpec(keyBytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(keyBytes));
        properties.setIssuer("ledgerflow");
        properties.setTtl(java.time.Duration.ofMinutes(15));
        JwtConfiguration configuration = new JwtConfiguration();
        decoder = configuration.jwtDecoder(key, properties);
    }

    @Test
    void acceptsWellFormedToken() {
        String token = token("ledgerflow", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(300));
        assertEquals("c65e10a9-235c-4310-a8e8-15a596c844e1", decoder.decode(token).getSubject());
    }

    @Test
    void rejectsSignatureTampering() {
        String[] pieces = token("ledgerflow", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                Instant.now().minusSeconds(5), Instant.now().plusSeconds(300)).split("\\.");
        char replacement = pieces[2].charAt(0) == 'A' ? 'B' : 'A';
        pieces[2] = replacement + pieces[2].substring(1);
        assertRejected(String.join(".", pieces));
    }

    @Test
    void rejectsExpiredWrongIssuerWrongAudienceAndMalformedSubject() {
        Instant now = Instant.now();
        assertRejected(token("ledgerflow", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                now.minusSeconds(600), now.minusSeconds(1)));
        assertRejected(token("other-issuer", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                now.minusSeconds(1), now.plusSeconds(300)));
        assertRejected(token("ledgerflow", "other-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                now.minusSeconds(1), now.plusSeconds(300)));
        assertRejected(token("ledgerflow", "ledgerflow-api", "not-a-uuid",
                now.minusSeconds(1), now.plusSeconds(300)));
    }

    @Test
    void rejectsTokensMissingIssuedAtOrExpiration() {
        Instant now = Instant.now();
        assertRejected(token("ledgerflow", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                null, now.plusSeconds(300)));
        assertRejected(token("ledgerflow", "ledgerflow-api", "c65e10a9-235c-4310-a8e8-15a596c844e1",
                now.minusSeconds(1), null));
    }

    @Test
    void rejectsMissingAudienceAndNoncanonicalUuid() {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ledgerflow")
                .subject("c65e10a9-235c-4310-a8e8-15a596c844e1")
                .issuedAt(Instant.now().minusSeconds(1)).expiresAt(Instant.now().plusSeconds(60)).build();
        assertRejected(encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue());
        assertRejected(token("ledgerflow", "ledgerflow-api", "1-1-1-1-1", Instant.now().minusSeconds(1), Instant.now().plusSeconds(60)));
    }

    @Test
    void startupRejectsMissingWeakOrMalformedSecrets() {
        JwtConfiguration configuration = new JwtConfiguration();
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("ledgerflow"); properties.setTtl(java.time.Duration.ofMinutes(15));
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(properties));
        properties.setSecret("not base64!");
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(properties));
        properties.setSecret(Base64.getEncoder().encodeToString(new byte[16]));
        assertThrows(IllegalStateException.class, () -> configuration.jwtSecretKey(properties));
    }

    private String token(String issuer, String audience, String subject, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer(issuer).subject(subject)
                .audience(List.of(audience));
        if (issuedAt != null) claims.issuedAt(issuedAt);
        if (expiresAt != null) claims.expiresAt(expiresAt);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    private void assertRejected(String token) { assertThrows(JwtException.class, () -> decoder.decode(token)); }
}
