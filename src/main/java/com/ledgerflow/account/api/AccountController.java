package com.ledgerflow.account.api;

import com.ledgerflow.account.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public AccountView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateAccount request) {
        return accounts.create(UUID.fromString(jwt.getSubject()), request.name());
    }
    @GetMapping
    public List<AccountView> list(@AuthenticationPrincipal Jwt jwt) { return accounts.list(UUID.fromString(jwt.getSubject())); }
    @GetMapping("/{id}")
    public AccountView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return accounts.get(UUID.fromString(jwt.getSubject()), id); }
    @PatchMapping("/{id}/status")
    public AccountView status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ChangeStatus request) {
        return accounts.changeStatus(UUID.fromString(jwt.getSubject()), id, request.status());
    }
}
