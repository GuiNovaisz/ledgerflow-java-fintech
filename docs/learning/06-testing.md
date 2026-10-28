# 6. Testing the application

Tests provide evidence for specific behaviors. A unit test is fast and isolates a class; an integration test starts the Spring application and uses a real PostgreSQL engine. Neither a successful compile nor a test that did not execute proves the behavior it was meant to cover.

The current CSV unit test is an example of one test per behavior: the input contains commas, quotes, and a newline, and the assertion checks the exact escaped cell.

```java
@Test void quotesCommasNewlinesAndQuotes() {
    assertThat(ReportService.csvCell("Food, \"today\"\nOK")).isEqualTo("\"Food, \"\"today\"\"\nOK\"");
}
```

The test does not need Spring or PostgreSQL because `csvCell` is a static formatting function. By contrast, account ownership and locking need the application and database to be meaningful.

## Unit tests

`AccountTest`, `AuthServiceTest`, `JwtValidationTest`, and `CsvTest` focus on individual rules. They can check money validation, password behavior, token validation, and CSV safety without needing to launch the full app. Run all unit tests with:

```powershell
.\mvnw.cmd test
```

Run a focused class with `-Dtest=AccountTest`, for example. Keep each test specific: arrange inputs, perform one action, and assert the observable result.

## PostgreSQL integration tests

`LedgerFlowIT` exercises HTTP endpoints, Spring Security, migrations, and financial operations against PostgreSQL 16 using Testcontainers. It covers ownership, transfers, idempotency, rollback, CSV, and synchronized concurrency scenarios. Run it with:

```powershell
.\mvnw.cmd -Pintegration verify
```

This needs Docker. If Docker is unavailable and Testcontainers cannot start, the integration evidence is missing; do not describe it as passed. The test has an explicit external database option, but that also must be PostgreSQL 16.

## What to assert

For a financial operation, assert both the HTTP response and persisted effects: account balances, operation count, posting count, and rollback behavior. For an authorization boundary, check status and body for both owned and foreign identifiers. For concurrency, coordinate requests with latches so they overlap; sequential requests do not establish race safety.

## Practical changes (exactly three)

1. Add an account API integration test that accepts a name of exactly 100 characters and rejects 101 characters. The current integration test rejects a blank name but does not cover this length boundary. Verify with `./mvnw.cmd -Pintegration verify`.
2. Add a history API integration test that rejects `size=0` and `size=101`; the service caps page size at 100, but the current suite does not test those pagination limits. Verify with `./mvnw.cmd -Pintegration verify`.
3. Add a summary integration test for a newly created account over a valid period and assert income, expenses, and net are zero with an empty category map. Existing summary coverage uses a non-empty history. Verify with `./mvnw.cmd -Pintegration verify`.

## Interview check

**What should a unit test prove?** A small, focused rule of one unit without relying on external infrastructure.

**Why use Testcontainers here?** Database-specific locks, SQL, constraints, and migrations need validation against PostgreSQL, not only an in-memory substitute.

**How do you test concurrency?** Start independent requests together using synchronization, then assert the final persisted state and allowed outcomes rather than relying on timing or sequential calls.
