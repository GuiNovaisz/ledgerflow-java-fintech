package com.ledgerflow.goals;

import com.ledgerflow.shared.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** A user-maintained plan, deliberately separate from the immutable financial ledger. */
@Entity
@Table(name = "planning_goals")
public class Goal {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "target_amount", nullable = false, precision = 17, scale = 2) private BigDecimal targetAmount;
    @Column(name = "current_amount", nullable = false, precision = 17, scale = 2) private BigDecimal currentAmount;
    @Column(name = "due_date") private LocalDate dueDate;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Goal() { }
    Goal(UUID ownerId, String name, BigDecimal target, BigDecimal current, LocalDate dueDate) {
        this.id = UUID.randomUUID(); this.ownerId = ownerId; this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        change(name, target, current, dueDate);
    }
    void change(String name, BigDecimal target, BigDecimal current, LocalDate dueDate) {
        if (name == null || name.isBlank() || name.trim().length() > 100) throw new IllegalArgumentException("Goal name must have 1 to 100 characters");
        if (current == null || current.signum() < 0 || current.compareTo(Money.MAX) > 0 || current.scale() > 2) throw new IllegalArgumentException("Invalid current amount");
        if (dueDate != null && (dueDate.isBefore(LocalDate.of(1970, 1, 1)) || dueDate.isAfter(LocalDate.of(9999, 12, 31)))) throw new IllegalArgumentException("Invalid due date");
        this.name = name.trim(); this.targetAmount = Money.positive(target); this.currentAmount = current.setScale(2); this.dueDate = dueDate;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getCurrentAmount() { return currentAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public long getVersion() { return version; }
    public String getStatus() { return currentAmount.compareTo(targetAmount) >= 0 ? "COMPLETED" : "ACTIVE"; }
}
