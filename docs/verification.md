# Verification evidence

Verified locally on **2026-10-28**, with user-facing dates in America/Sao_Paulo. LedgerFlow is a financial simulation. This report records observed results, not performance or production-readiness claims.

## Environment and reproducibility

- Windows, PowerShell 7, Temurin **21.0.12.1+1** (`java` and `javac` from the same JDK).
- Maven **3.9.9**, official Maven Wrapper **3.3.4**, pinned distribution SHA-256.
- Spring Boot **3.5.16**, Springdoc **2.10.0**.
- Docker Engine **29.8.1**, Compose **5.5.1**, PostgreSQL **16** (`postgres:16-alpine`).
- The machine's default Java was Java 8. A portable JDK 21 was downloaded into ignored `.tools/` and verified against Adoptium's SHA-256. No persistent Java/PATH setting was changed.
- Docker was initially unavailable. Starting the installed Docker service through its CLI made it available; tests then used real containers. No database substitute was used.

For this workspace, select the downloaded JDK in the current PowerShell process:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools\jdk21\jdk-21.0.12.1+1').Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd -B -ntp -Pintegration verify
```

For another checkout, install a JDK 21 and set `JAVA_HOME` to its directory. The ignored `.tools/` directory is not part of the repository. Docker must be available for the integration profile. `mvnw.cmd test` runs only unit tests; it is not a substitute for `-Pintegration verify`.

## Automated tests

**BUILD SUCCESS: 37 tests, 0 failures, 0 errors, 0 skipped.** The machine-readable summary was derived from Maven's Surefire and Failsafe XML reports: [test-summary.json](evidence/test-summary.json).

| Suite | Tests | What was checked |
| --- | ---: | --- |
| AccountTest | 7 | Exact decimals, insufficient funds, precision/range, balance overflow and inactive accounts |
| AuthServiceTest | 6 | Normalized identity, encoded password persistence, duplicates, generic login failures, UTF-8 bounds and BCrypt suffix rejection |
| CsvTest | 9 | CSV quoting, embedded delimiters/newlines and formula-like text |
| JwtValidationTest | 6 | Valid token, tampering, expiry, issuer/audience/subject, missing claims, signing-key configuration |
| LedgerFlowIT | 9 | Real PostgreSQL/Flyway persistence, registration/login, isolation, transfers, reports, retries, concurrent spending, opposite transfers, rollback, immutability and HTTP/OpenAPI contracts |

Integration tests use independent requests synchronized with latches and bounded waits. Two withdrawals competing for BRL 100.00 can commit only one BRL 80.00 withdrawal. Concurrent retries share one operation. Opposite transfers conserve the combined balance. This is a correctness check with small concurrency, not a load test.

The rollback test deliberately adds a temporary PostgreSQL trigger that rejects a recipient credit after work has started. It verifies that the source balance and operation table remain unchanged, then removes the trigger. The expected `forced integration rollback` error in the test log is evidence of this injected failure, not a failed test.

## Container and HTTP execution

These commands succeeded locally:

```powershell
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 120
.\scripts\Verify-Api.ps1 -OutputPath docs/evidence/http-smoke.json
docker compose ps
```

Both services reached `healthy`. The runtime image uses UID/GID 10001. The app and database publish only to `127.0.0.1`; no public deployment occurred. Readiness includes the database health indicator. The final image was rebuilt after the final application edits.

The HTTP script creates synthetic users and accounts, generates its password in memory, and never records bearer tokens or passwords. [http-smoke.json](evidence/http-smoke.json) contains actual sanitized API responses, including an operation, cash-flow summary, CSV data, and assertions. It verifies a BRL 100.00 deposit, a BRL 35.25 transfer between users, exact idempotent replay, a BRL 10.00 withdrawal, account isolation, incoming history, export safety and inactive-account rejection.

The database check confirms Flyway migration V1 succeeded and reconciles each account's cached balance with credits minus debits. [database-check.json](evidence/database-check.json) records zero unreconciled accounts and zero unbalanced internal transfers in the local sample data.

## Review and corrections

A separate GPT-6.1 Sol / High agent reviewed the financial and security code. Findings corrected and checked include missing JWT audience handling, BCrypt's 72-byte boundary, required signed timestamps, PostgreSQL microsecond timestamp normalization for stable replay, correct HTTP error statuses/headers, and authenticated OpenAPI operations. This is a source review, not an independent penetration test or certification.

The local environment generator refuses to overwrite an existing `.env`. Git ignores `.env`, `.tools`, `.local`, build outputs and logs. No operational credentials are committed. Passwords in request examples and unit/integration tests are explicit synthetic fixtures, never configured production identities. Signing keys for integration tests are generated per run.

## Limits of this evidence

- The GitHub Actions workflow is provided and its build/test/Compose commands were exercised locally. It has **not** been run on GitHub; no repository was published or pushed.
- The Docker image build skips tests because the full suite runs separately before image validation. Image build success alone is not the test result reported above.
- No throughput, latency, availability, penetration-test or stress-test claims are made.
- Container tags select major versions rather than immutable image digests. Rebuilds can receive compatible image updates.
- Local app and migration access share a database role. Separate migration/runtime privileges, secret management, TLS termination, rate limits and backups are future operational work.
- JWT has a 15-minute lifetime and zero timestamp skew; use synchronized clocks. No refresh token or immediate revocation is implemented.
- Cash-flow summaries include transfers. Deposits and withdrawals have one local posting and do not represent external settlement or a complete bank general ledger.

Raw local build reports remain in ignored `target/surefire-reports`, `target/failsafe-reports` and `.tools/verification.log`. Re-running the HTTP script adds synthetic sample data; it does not clear an existing database.
