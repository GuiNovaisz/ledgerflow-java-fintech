# 4. Database design and transactions

The database is part of the financial behavior, not just a place to store objects. LedgerFlow uses PostgreSQL 16, Flyway SQL, foreign keys, unique constraints, check constraints, and transaction-scoped locks.

For example, the current migration declares the account currency and balance rules directly in PostgreSQL:

```sql
currency VARCHAR(3) NOT NULL DEFAULT 'BRL' CHECK (currency = 'BRL'),
balance NUMERIC(17,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
```

`CHECK` rejects a row that violates the expression even if a future application code path forgets to validate it. `NUMERIC(17,2)` stores a decimal with two fractional digits and up to 15 integer digits.

## Four core tables

`app_users` stores normalized email and a password hash. `accounts` stores an owner, currency, status, and current balance. `operations` records the business request and its idempotency key. `ledger_entries` records the debit or credit effect on each account. Foreign keys preserve relationships, and database triggers make operations and postings append-only.

A simulated deposit creates one operation and one credit entry. A withdrawal creates one operation and one debit entry. A transfer creates one operation and a debit plus a credit of the same amount. The account balance is a cached current value; postings provide an audit trail. The design does not add external clearing accounts, so it is not a complete general ledger.

## Atomicity and row locking

Atomicity means either every database change for a transfer commits or none does. A transfer could otherwise debit the sender and fail before crediting the recipient. `@Transactional` and PostgreSQL rollback prevent that partial outcome.

Two withdrawals must not both spend the same starting funds. The service locks account rows with `SELECT ... FOR UPDATE` before checking and changing balances. Multiple account rows are locked in sorted UUID order to reduce deadlocks. Account status changes use the same row lock, so an account cannot be deactivated midway through a posting.

## Idempotency

Clients may not know whether a timed-out request reached the server. The caller supplies an idempotency key; a transaction-scoped PostgreSQL advisory lock serializes simultaneous uses of the same owner/key. The service checks the saved request fingerprint. An identical retry returns the original operation, while a different semantic request with that key returns a conflict. A database unique constraint is the final duplicate guard. The key format is `[A-Za-z0-9._:-]{1,100}`.

Use the exact same logical body and key when retrying. Do not generate a fresh key for a retry, or the server will treat it as a new operation.

## Read consistency and time

History is bounded and ordered by timestamp plus unique identifier. Date filters are instants in UTC with a lower-inclusive, upper-exclusive range `[from,to)`. This avoids ambiguous local-time boundaries and makes adjacent periods fit without overlap. A separately requested CSV is a new read and can differ if operations commit between calls.

## Practical changes (exactly three)

1. Add an integration assertion that a direct SQL update setting an account balance to `-0.01` fails the database check. The current tests exercise service-level overdraft protection but not this SQL constraint directly. Verify with `./mvnw.cmd -Pintegration verify` and Docker available.
2. In `LedgerFlowIT`, register two users and prove they may each successfully use the same idempotency key for separate deposits. The current retry test uses one owner only. Verify that each owner has one operation with that key using the integration profile.
3. Add an integration assertion that a direct SQL update to currency `USD` fails the schema check, demonstrating the database's BRL-only invariant independently of request validation. Verify with `./mvnw.cmd -Pintegration verify` and Docker available.

## Interview check

**Why combine an account row lock with a transaction?** The lock prevents concurrent writers from making decisions from the same stale balance; the transaction commits the balance and its ledger records atomically.

**What is idempotency?** Repeating the same logical request with the same key returns its original successful result instead of applying the financial effect again.

**Why use `[from,to)` for dates?** The start is included and the end excluded, so neighboring periods can share a boundary without double-counting an event at that instant.
