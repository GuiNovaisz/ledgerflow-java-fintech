package com.ledgerflow.account.api;

import com.ledgerflow.account.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.ledgerflow.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.ledgerflow.account.api.AccountDtos.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts = accounts; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@AuthenticatedUser UUID userId, @Valid @RequestBody CreateAccount request) {
        return accounts.create(userId, request.name());
    }
    @GetMapping
    public List<AccountView> list(@AuthenticatedUser UUID userId) { return accounts.list(userId); }
    @GetMapping("/{id}")
    public AccountView get(@AuthenticatedUser UUID userId, @PathVariable UUID id) { return accounts.get(userId, id); }
    @PatchMapping("/{id}/status")
    public AccountView status(@AuthenticatedUser UUID userId, @PathVariable UUID id, @Valid @RequestBody ChangeStatus request) {
        return accounts.changeStatus(userId, id, request.status());
    }
}
