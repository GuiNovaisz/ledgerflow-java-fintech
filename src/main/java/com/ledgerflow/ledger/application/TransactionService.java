package com.ledgerflow.ledger.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.account.application.AccountService;
import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.persistence.AccountRepository;
import com.ledgerflow.ledger.api.TransactionDtos.OperationView;
import com.ledgerflow.ledger.domain.*;
import com.ledgerflow.ledger.persistence.*;
import com.ledgerflow.shared.BusinessException;
import com.ledgerflow.shared.Money;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class TransactionService {
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final LedgerEntryRepository entries;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public TransactionService(AccountRepository accounts, OperationRepository operations,
            LedgerEntryRepository entries, JdbcTemplate jdbc, ObjectMapper json) {
        this.accounts = accounts; this.operations = operations; this.entries = entries; this.jdbc = jdbc; this.json = json;
    }

    @Transactional(timeout = 15)
    public OperationView execute(UUID owner, String key, Operation.Type type, UUID source, UUID target,
                                 BigDecimal amount, Category category, String description) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}")) throw new IllegalArgumentException("Invalid idempotency key");
        amount = Money.positive(amount);
        if (category == null || description == null || description.length() > 255) throw new IllegalArgumentException("Invalid metadata");
        validateShape(type, source, target);
        String fingerprint = fingerprint(type, source, target, amount, category, description);
        // Transaction-scoped PostgreSQL lock: releases on commit/rollback, works across app instances.
        long lockId = ByteBuffer.wrap(digest(owner + ":" + key)).getLong();
        jdbc.query("select pg_advisory_xact_lock(?)", rs -> { }, lockId);
        Optional<Operation> previous = operations.findByOwnerIdAndIdempotencyKey(owner, key);
        if (previous.isPresent()) {
            if (!previous.get().getFingerprint().equals(fingerprint))
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency key already used with a different request.");
            return OperationView.from(previous.get());
        }

        // All writers, including status changes, lock the same account rows. Global order avoids deadlocks.
        Map<UUID, Account> locked = new HashMap<>();
        List<UUID> ids = new ArrayList<>();
        if (source != null) ids.add(source);
        if (target != null) ids.add(target);
        ids.sort(Comparator.naturalOrder());
        for (UUID id : ids) locked.put(id, accounts.lockById(id).orElseThrow(AccountService::notFound));
        Account owned = locked.get(source == null ? target : source);
        if (!owned.getOwnerId().equals(owner)) throw AccountService.notFound();
        for (Account account : locked.values()) account.ensureActive();
        if (source != null && target != null && !locked.get(source).getCurrency().equals(locked.get(target).getCurrency()))
            throw new IllegalArgumentException("Currency mismatch");

        if (source != null) locked.get(source).debit(amount);
        if (target != null) locked.get(target).credit(amount);
        Operation operation = operations.save(new Operation(owner, key, fingerprint, type, source, target, amount, category, description));
        if (source != null) entries.save(new LedgerEntry(operation, source, LedgerEntry.Direction.DEBIT));
        if (target != null) entries.save(new LedgerEntry(operation, target, LedgerEntry.Direction.CREDIT));
        // The proxy commits before returning HTTP success. Any failure also rolls back the balances.
        return OperationView.from(operation);
    }

    private void validateShape(Operation.Type type, UUID source, UUID target) {
        boolean valid = type != null && switch (type) {
            case DEPOSIT -> source == null && target != null;
            case WITHDRAWAL -> source != null && target == null;
            case TRANSFER -> source != null && target != null && !source.equals(target);
        };
        if (!valid) throw new IllegalArgumentException("Invalid source or target");
    }
    private String fingerprint(Operation.Type type, UUID source, UUID target, BigDecimal amount, Category category, String description) {
        try {
            String canonical = json.writeValueAsString(Arrays.asList(type, source, target, amount.toPlainString(), category, description));
            return HexFormat.of().formatHex(digest(canonical));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot fingerprint request", e); }
    }
    private static byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
