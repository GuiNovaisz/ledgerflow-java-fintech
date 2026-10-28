package com.ledgerflow.ledger.api;

import com.ledgerflow.ledger.application.TransactionService;
import com.ledgerflow.ledger.domain.Operation.Type;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.ledgerflow.ledger.api.TransactionDtos.*;

@RestController @RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }
    @PostMapping("/deposits") @ResponseStatus(HttpStatus.CREATED)
    public OperationView deposit(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CashRequest r) {
        return service.execute(UUID.fromString(jwt.getSubject()), key, Type.DEPOSIT, null, r.accountId(), r.amount(), r.category(), r.description());
    }
    @PostMapping("/withdrawals") @ResponseStatus(HttpStatus.CREATED)
    public OperationView withdraw(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CashRequest r) {
        return service.execute(UUID.fromString(jwt.getSubject()), key, Type.WITHDRAWAL, r.accountId(), null, r.amount(), r.category(), r.description());
    }
    @PostMapping("/transfers") @ResponseStatus(HttpStatus.CREATED)
    public OperationView transfer(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest r) {
        return service.execute(UUID.fromString(jwt.getSubject()), key, Type.TRANSFER, r.sourceAccountId(), r.targetAccountId(), r.amount(), r.category(), r.description());
    }
}
