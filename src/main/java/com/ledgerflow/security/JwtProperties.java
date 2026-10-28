package com.ledgerflow.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ledgerflow.jwt")
public class JwtProperties {
    private String secret;
    private String issuer = "ledgerflow";
    private Duration ttl = Duration.ofMinutes(15);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
}
