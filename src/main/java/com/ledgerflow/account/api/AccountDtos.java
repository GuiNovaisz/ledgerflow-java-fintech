package com.ledgerflow.account.api;

import com.ledgerflow.account.domain.Account;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() { }
    public record CreateAccount(@NotBlank @Size(max = 100) String name) { }
    public record ChangeStatus(@NotNull Account.Status status) { }
    public record AccountView(UUID id, String name, String currency, BigDecimal balance, Account.Status status, Instant createdAt) {
        public static AccountView from(Account a) {
            return new AccountView(a.getId(), a.getName(), a.getCurrency(), a.getBalance(), a.getStatus(), a.getCreatedAt());
        }
    }
}
