package com.ledgerflow.ledger.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Immutable @Table(name = "ledger_entries")
public class LedgerEntry {
    public enum Direction { DEBIT, CREDIT }
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "operation_id") private Operation operation;
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 6) private Direction direction;
    @Column(nullable = false, precision = 17, scale = 2) private BigDecimal amount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected LedgerEntry() { }
    public LedgerEntry(Operation operation, UUID accountId, Direction direction) {
        this.id = UUID.randomUUID(); this.operation = operation; this.accountId = accountId;
        this.direction = direction; this.amount = operation.getAmount(); this.createdAt = operation.getCreatedAt();
    }
    public UUID getId() { return id; }
    public Operation getOperation() { return operation; }
    public Direction getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
