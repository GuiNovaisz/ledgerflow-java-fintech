package com.ledgerflow.account.domain;

import com.ledgerflow.shared.BusinessException;
import com.ledgerflow.shared.Money;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    public enum Status { ACTIVE, INACTIVE }
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, precision = 17, scale = 2) private BigDecimal balance;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Status status;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Account() { }
    public Account(UUID ownerId, String name) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name.trim();
        this.currency = "BRL";
        this.balance = new BigDecimal("0.00");
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public void ensureActive() {
        if (status != Status.ACTIVE) throw new BusinessException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Account must be active.");
    }
    public void credit(BigDecimal amount) {
        ensureActive();
        BigDecimal next = balance.add(Money.positive(amount));
        if (next.compareTo(Money.MAX) > 0) throw new BusinessException(HttpStatus.CONFLICT, "BALANCE_LIMIT", "Account balance limit reached.");
        balance = next;
    }
    public void debit(BigDecimal amount) {
        ensureActive();
        amount = Money.positive(amount);
        if (balance.compareTo(amount) < 0) throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient funds.");
        balance = balance.subtract(amount);
    }
    public void changeStatus(Status status) { this.status = status; }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
