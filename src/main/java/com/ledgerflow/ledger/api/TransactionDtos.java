package com.ledgerflow.ledger.api;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.Operation;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransactionDtos {
    private TransactionDtos() { }
    public record CashRequest(@NotNull UUID accountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotNull Category category, @NotNull @Size(max = 255) String description) { }
    public record TransferRequest(@NotNull UUID sourceAccountId, @NotNull UUID targetAccountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotNull Category category, @NotNull @Size(max = 255) String description) { }
    public record OperationView(UUID id, Operation.Type type, UUID sourceAccountId, UUID targetAccountId,
            BigDecimal amount, Category category, String description, Instant createdAt) {
        public static OperationView from(Operation op) {
            return new OperationView(op.getId(), op.getType(), op.getSourceAccountId(), op.getTargetAccountId(),
                    op.getAmount(), op.getCategory(), op.getDescription(), op.getCreatedAt());
        }
    }
}
