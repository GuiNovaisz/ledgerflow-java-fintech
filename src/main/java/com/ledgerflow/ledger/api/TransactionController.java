package com.ledgerflow.ledger.api;

import com.ledgerflow.ledger.application.TransactionService;
import com.ledgerflow.ledger.domain.Operation.Type;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.ledgerflow.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.ledgerflow.ledger.api.TransactionDtos.*;

@RestController @RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }
    @PostMapping("/deposits") @ResponseStatus(HttpStatus.CREATED)
    public OperationView deposit(@AuthenticatedUser UUID userId, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CashRequest r) {
        return service.execute(userId, key, Type.DEPOSIT, null, r.accountId(), r.amount(), r.category(), r.description());
    }
    @PostMapping("/withdrawals") @ResponseStatus(HttpStatus.CREATED)
    public OperationView withdraw(@AuthenticatedUser UUID userId, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CashRequest r) {
        return service.execute(userId, key, Type.WITHDRAWAL, r.accountId(), null, r.amount(), r.category(), r.description());
    }
    @PostMapping("/transfers") @ResponseStatus(HttpStatus.CREATED)
    public OperationView transfer(@AuthenticatedUser UUID userId, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest r) {
        return service.execute(userId, key, Type.TRANSFER, r.sourceAccountId(), r.targetAccountId(), r.amount(), r.category(), r.description());
    }
}
