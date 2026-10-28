# 1. Java fundamentals through LedgerFlow

You can follow this chapter without knowing object-oriented programming. We will read a few lines of existing Java, name each piece, and make small changes in the project. Source files are under `src/main/java/com/ledgerflow`; tests are under `src/test/java/com/ledgerflow`.

## Start with concrete values

Every value has a **type**. `String` stores text, `UUID` represents an identifier, `Instant` represents a point in time, and `BigDecimal` represents decimal amounts. A **variable** is a name for a value. In the real `Account` constructor below, `ownerId` and `name` are parameters: values provided when the method is called. `this.name` means the `name` field belonging to the object being created.

```java
public Account(UUID ownerId, String name) {
    this.id = UUID.randomUUID();
    this.ownerId = ownerId;
    this.name = name.trim();
    this.currency = "BRL";
    this.balance = new BigDecimal("0.00");
    this.status = Status.ACTIVE;
    this.createdAt = Instant.now();
}
```

This **constructor** sets the initial values for a new account. `new Account(id, "Everyday")` calls it. In Java, `new` creates an object from a class. The constructor always starts the account with a zero balance and active status.

## Read a method and an `if`

A **method** is a named block of code you can call. Its parameters are the inputs in parentheses; its return type (here `void`) says it does not return a value. This excerpt is from the real `Account.debit` method:

```java
public void debit(BigDecimal amount) {
    ensureActive();
    amount = Money.positive(amount);
    if (balance.compareTo(amount) < 0) throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient funds.");
    balance = balance.subtract(amount);
}
```

An `if` chooses what to do based on a true/false condition. Here, the exception is thrown only when the balance is less than the requested amount. Otherwise, execution continues to `subtract`. The method receives `amount` as a parameter and uses the object's existing `balance` field.

`Account.Status` and `Operation.Type` are **enums**: a type with a fixed set of named choices. A status is `ACTIVE` or `INACTIVE`; an operation type is `DEPOSIT`, `WITHDRAWAL`, or `TRANSFER`. Enums prevent misspelled free-form strings from representing these choices.

## Decimal values and money

`BigDecimal` is immutable: methods such as `add` and `subtract` return a new value instead of changing the old one. That is why the code assigns the result back to `balance`. Avoid `double` for money because binary floating point cannot exactly represent many decimal fractions.

The real helper `Money.positive` rejects zero, negative amounts, more than 15 integer digits, and values with more than two fractional digits. It then sets scale to two with `RoundingMode.UNNECESSARY`, which rejects rounding. For example, `new BigDecimal("1.001")` is rejected rather than rounded to `1.00`.

## What the project structure means

A **class** defines fields and methods; an **object** is one instance of that class. `Account` is a class; a user's account is an object. A **record**, such as `CashRequest`, is a compact type for carrying a fixed set of values. A **package** groups related classes: `ledger.api` handles HTTP input/output, while `ledger.application` coordinates the financial use case. You can understand the method calls before studying OOP terminology in chapter 2.

For a deposit, `TransactionController.deposit` receives the HTTP request and authenticated JWT, then calls `TransactionService.execute`. The service checks idempotency, locks the account, calls `Account.credit`, and saves an operation and posting in one transaction.

## Practical changes (exactly three)

1. Add `Money.format(BigDecimal value)` that returns `Money.positive(value).toPlainString()`. Create `MoneyTest` and check that `new BigDecimal("12.5")` formats as `"12.50"` and `new BigDecimal("0.01")` as `"0.01"`. Verify with `./mvnw.cmd -Dtest=MoneyTest test`.
2. In `AccountDtos`, declare `public static final int MAX_NAME_LENGTH = 100` and use it in `@Size(max = MAX_NAME_LENGTH)` instead of the literal. This is a compile-time constant, so Java permits it in an annotation. Verify with `./mvnw.cmd -DskipTests compile` and confirm the endpoint still documents a 100-character limit.
3. Add a test to `AccountTest` constructing `new Account(owner, "  Savings  ")` and asserting the stored name is `"Savings"`, the status is `ACTIVE`, and the initial balance is zero. The current test class does not check constructor defaults or trimming. Verify with `./mvnw.cmd -Dtest=AccountTest test`.

## Interview check

**What is the difference between a class and an object?** A class defines fields and behavior; an object is one runtime instance with its own field values.

**Why use `BigDecimal` for money?** It models decimal values and gives explicit scale and rounding behavior. LedgerFlow rejects fractions of a cent instead of silently rounding user input.

**What does the `if` in `debit` do?** It compares the current balance to the requested amount and throws a conflict before changing the balance if there are insufficient funds.
