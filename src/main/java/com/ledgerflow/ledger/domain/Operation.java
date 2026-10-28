package com.ledgerflow.ledger.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Immutable @Table(name = "operations")
public class Operation {
    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER }
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Type type;
    @Column(name = "source_account_id") private UUID sourceAccountId;
    @Column(name = "target_account_id") private UUID targetAccountId;
    @Column(nullable = false, precision = 17, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Category category;
    @Column(nullable = false, length = 255) private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Operation() { }
    public Operation(UUID owner, String key, String fingerprint, Type type, UUID source, UUID target,
                     BigDecimal amount, Category category, String description) {
        this.id = UUID.randomUUID(); this.ownerId = owner; this.idempotencyKey = key;
        this.fingerprint = fingerprint; this.type = type; this.sourceAccountId = source;
        this.targetAccountId = target; this.amount = amount; this.category = category;
        this.description = description; this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public UUID getId() { return id; }
    public String getFingerprint() { return fingerprint; }
    public Type getType() { return type; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
