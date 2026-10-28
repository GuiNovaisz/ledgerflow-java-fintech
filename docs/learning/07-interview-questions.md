# 7. Interview questions and expected answers

Use these prompts to practice explaining the design in plain language. The answer sketches describe this repository, not universal rules.

Here is the retry decision in `TransactionService.execute`:

```java
if (previous.isPresent()) {
    if (!previous.get().getFingerprint().equals(fingerprint))
        throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency key already used with a different request.");
    return OperationView.from(previous.get());
}
```

The saved fingerprint represents the meaning of the earlier request. Matching requests return the earlier operation; a changed request cannot reuse its key.

## Java and application structure

**1. How does a request move through LedgerFlow?**

Spring Security validates the bearer token; a controller parses and validates HTTP input; a service coordinates the use case; domain objects enforce local rules; repositories persist the result; the response DTO is serialized to JSON.

**2. What is the difference between a DTO and a domain object?**

A DTO is shaped for input or output at the API boundary. `CashRequest` is transport input; `Account` owns account state and behavior.

**3. Why is `BigDecimal` used?**

It supports decimal monetary values and explicit scale rules. The service rejects excess precision instead of silently rounding cents.

## Finance and database

**4. How does a transfer stay atomic?**

One database transaction locks both accounts, checks status/currency/funds, changes balances, and inserts one operation plus two postings. Any failure rolls back all these writes.

**5. How are concurrent withdrawals prevented from overspending?**

The service locks the account row before checking funds. A competing transaction waits, then checks the updated committed balance.

**6. How does idempotency handle a retry?**

The owner and key identify a previous success. A transaction-scoped advisory lock serializes competing uses; the canonical request fingerprint must match or the API returns conflict. A unique constraint is the final guard.

**7. Why use both a current account balance and ledger postings?**

The balance makes current reads efficient, while immutable postings record how it changed. Service rules and integration checks keep them aligned; the schema is not a full external double-entry accounting system.

**8. Why lock multiple account rows in sorted order?**

Opposite-direction transfers can otherwise acquire the same rows in opposite order and deadlock. A consistent order reduces that risk.

## Security and operations

**9. How does the application prevent a caller from choosing another owner?**

The owner ID comes from the verified JWT subject, never from the request body. Account access is scoped to that identity.

**10. Can a user transfer to another user's account? What is disclosed?**

Yes, if the recipient account exists and is active. The response does not expose recipient profile data or balance. The recipient can see the incoming posting in their own history.

**11. What does the CSV protection address?**

Spreadsheet applications may interpret user-provided cells as formulas. The export neutralizes formula-like text and performs CSV escaping; exports are also capped at 10,000 rows.

**12. What are the important production gaps?**

Only BRL and simulated cash operations exist. There are no real bank/payment integrations, refresh or immediate token revocation, rate limiting, MFA, KYC/AML, or external clearing accounts. Public deployment is not claimed.

## Practical changes (exactly three)

1. Pick one database or security question and write a 5-sentence answer, then add the source file path and method that support each claim. Verify every statement against the current implementation.
2. Draw a five-step withdrawal sequence—verified user, idempotency check, account lock, balance change, commit—and compare it with [the architecture guide](../architecture.md). Correct any step that does not match the actual service order.
3. Add one new interview question and a source-backed expected answer to this chapter, then check that its code link or file reference resolves and that the answer does not claim an unimplemented feature.
