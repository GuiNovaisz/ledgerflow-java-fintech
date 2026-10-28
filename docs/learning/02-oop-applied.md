# 2. Object-oriented programming applied

Object-oriented programming (OOP) is a way to put related state and behavior together, while giving each part a clear boundary. LedgerFlow uses a practical, modest form of OOP rather than a class for every idea.

Here is a short excerpt from the existing `TransactionService` constructor:

```java
public TransactionService(AccountRepository accounts, OperationRepository operations,
            LedgerEntryRepository entries, JdbcTemplate jdbc, ObjectMapper json) {
        this.accounts = accounts; this.operations = operations; this.entries = entries; this.jdbc = jdbc; this.json = json;
}
```

These parameters are collaborators: the service asks them to load accounts, save operations, write ledger entries, run SQL, and encode JSON. `this.accounts` is a field on the service; `accounts` is the value passed to the constructor.

## Encapsulation: protect state through behavior

`Account` keeps `balance` private. Callers cannot assign an arbitrary balance; they use `credit` or `debit`. Those methods call `Money.positive`, check the account is active, enforce the balance cap, or reject insufficient funds. This is **encapsulation**: the object controls how its state changes so its rules are not bypassed by ordinary application code.

The database adds a second line of defense (`CHECK (balance >= 0)`). The service also locks the account row before applying changes. Object rules alone do not solve multi-request concurrency; each layer protects a different boundary.

## Composition: build a use case from collaborators

`TransactionService` is composed with repositories, `JdbcTemplate`, and `ObjectMapper`. It coordinates these collaborators to complete one operation. It does not inherit from a large generic transaction framework. Spring injects the dependencies through the constructor, making the required collaborators visible and replaceable in tests.

The controller delegates to the service instead of reaching into repositories. This is a boundary: HTTP parsing belongs in the API package; transaction policy belongs in the application service; balance invariants belong on `Account`.

## Interfaces and polymorphism

An interface is a contract that multiple implementations can satisfy. The service depends on repository contracts rather than SQL details for most reads and writes. Spring Data creates their runtime implementations. The code also uses enums such as `Operation.Type` to limit valid operation kinds. Polymorphism is useful when alternatives genuinely vary; it is not necessary to introduce an interface for every single class.

## Immutability and records

`CashRequest`, `TransferRequest`, and `OperationView` are Java records: their components are final and provide generated accessors. `Operation` and `LedgerEntry` are marked immutable for ORM updates, and PostgreSQL triggers reject updates and deletes. `Account` is mutable because its current balance and status change, but only through methods that express those changes.

## Practical changes (exactly three)

1. Make `Account.changeStatus(null)` fail immediately with `Objects.requireNonNull(status)` and add a unit test that checks the exception. This prevents invalid in-memory state before persistence. Verify with `./mvnw.cmd -Dtest=AccountTest test`.
2. Add an `AccountTest` case that credits `25.00`, deactivates and reactivates the account, then confirms it remains active with `25.00`. The integration suite changes status but does not assert this preservation. Verify with `./mvnw.cmd -Dtest=AccountTest test`.
3. Add a focused test for `AccountView.from(account)` that checks the account ID, name, currency, balance, status, and timestamp are mapped to the record. There is no current unit test for this mapper. Verify with `./mvnw.cmd -Dtest=AccountDtosTest test` after adding that test class.

## Interview check

**What does encapsulation provide here?** It keeps balance changes behind domain methods that enforce positivity, account status, and funds rules.

**Why prefer composition over inheritance for `TransactionService`?** The service coordinates focused collaborators; it does not need to be a specialized subtype of a broad base class. Constructor injection also makes dependencies explicit.

**Are immutable entities enough for financial correctness?** No. Database constraints, transaction boundaries, account row locks, and integration tests handle persistence and concurrent requests as well.
