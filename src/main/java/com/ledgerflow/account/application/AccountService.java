package com.ledgerflow.account.application;

import com.ledgerflow.account.api.AccountDtos.AccountView;
import com.ledgerflow.account.domain.Account;
import com.ledgerflow.account.persistence.AccountRepository;
import com.ledgerflow.shared.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;
    public AccountService(AccountRepository accounts) { this.accounts = accounts; }
    @Transactional
    public AccountView create(UUID owner, String name) { return AccountView.from(accounts.save(new Account(owner, name))); }
    public AccountView get(UUID owner, UUID id) { return AccountView.from(requireOwned(owner, id)); }
    public List<AccountView> list(UUID owner) { return accounts.findByOwnerIdOrderByCreatedAtAscIdAsc(owner).stream().map(AccountView::from).toList(); }
    public Account requireOwned(UUID owner, UUID id) {
        return accounts.findByIdAndOwnerId(id, owner).orElseThrow(AccountService::notFound);
    }
    @Transactional(timeout = 15)
    public AccountView changeStatus(UUID owner, UUID id, Account.Status status) {
        Account account = accounts.lockById(id).orElseThrow(AccountService::notFound);
        if (!account.getOwnerId().equals(owner)) throw notFound();
        account.changeStatus(status);
        return AccountView.from(account);
    }
    public static BusinessException notFound() { return new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."); }
}
