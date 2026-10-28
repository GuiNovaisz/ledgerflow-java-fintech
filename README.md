# LedgerFlow

LedgerFlow is an educational fintech simulation built with Java 21, Spring Boot 3.5, Maven, and PostgreSQL 16. It provides authentication, BRL accounts, simulated deposits and withdrawals, transfers, history, summaries, and CSV statements.

> **Simulation only.** Deposits and withdrawals do not connect to banks, cards, payment providers, settlement rails, or real money. This repository is not a production banking platform.

[Português (Brasil)](README.pt-BR.md) · [Architecture](docs/architecture.md) · [Diagrams](docs/diagrams.md) · [API examples](docs/requests.http) · [Learning path](docs/learning/01-java-fundamentals.md) · [Verification evidence](docs/verification.md)

Verified locally: **37 automated tests passed**, PostgreSQL migrations applied, Docker Compose healthy, and the real HTTP demo passed. See the evidence link for scope and limitations.

## Included

- Stateless bearer JWT authentication and BCrypt password hashing.
- Owner-scoped BRL accounts with activation and deactivation.
- Deposits, withdrawals, and transfers recorded as immutable ledger postings.
- PostgreSQL transactions, row locks, and idempotency for financial requests.
- Paginated history, UTC date-range summaries, and formula-neutralized CSV exports.
- Flyway-managed schema and a Docker Compose local environment.

## Requirements

Windows PowerShell, Docker Desktop (or another Docker-compatible engine), and network access to fetch build dependencies and container images. JDK 21 is required for host-side Maven builds and tests, but not installed on the host for the Docker Compose startup: the Dockerfile builds with a JDK 21 image and runs on a Java 21 runtime image. Maven is provided by `mvnw.cmd`.

Confirm `java -version` reports Java 21. If the machine defaults to an older JDK, set `JAVA_HOME` for this PowerShell process only and prepend its `bin` directory to `PATH`:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

This does not change machine-wide Java settings.

## Start locally

From the repository root:

```powershell
.\scripts\New-LocalEnvironment.ps1
docker compose up --build -d --wait
```

The script creates `.env` with random secrets and refuses to overwrite an existing path. `.env` is ignored by Git. Compose binds PostgreSQL to `127.0.0.1:5432` and the application to `127.0.0.1:8080` by default; set `APP_PORT` in `.env` to change the app's host port. Inside Compose, the database is `postgres:5432`.

Compose reads `.env` for container configuration. A standalone Spring Boot process does **not** load `.env` automatically: export `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET` in that process's environment. The local database defaults in `application.yml` use `localhost:5432` and `ledgerflow`; provide `DB_PASSWORD` and `JWT_SECRET` explicitly.

API documentation: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). Compose checks `/actuator/health/readiness`.

Stop containers while preserving the local database volume:

```powershell
docker compose down
```

## Verify

Unit tests (JDK 21):

```powershell
.\mvnw.cmd test
```

Integration profile with PostgreSQL 16 via Testcontainers:

```powershell
.\mvnw.cmd -Pintegration verify
```

The integration profile requires a working Docker-compatible engine and fails if Testcontainers cannot start PostgreSQL. An unavailable or skipped integration suite is not verified. See [docs/verification.md](docs/verification.md) for observed results and the limits of the local verification.

After starting Compose, exercise the real HTTP API with PowerShell 7:

```powershell
.\scripts\Verify-Api.ps1
```

This creates synthetic sample data and prints sanitized results. Repeated runs add sample users and accounts; they never clear an existing database.

## API overview

Public endpoints: `POST /api/v1/auth/register` and `POST /api/v1/auth/login`. Other `/api/**` routes require `Authorization: Bearer <accessToken>`.

| Capability | Routes |
| --- | --- |
| Accounts | `POST /api/v1/accounts`, `GET /api/v1/accounts`, `GET /api/v1/accounts/{id}`, `PATCH /api/v1/accounts/{id}/status` |
| Financial operations | `POST /api/v1/transactions/deposits`, `/withdrawals`, `/transfers` |
| Account reporting | `GET /api/v1/accounts/{id}/entries`, `/summary`, `/statement.csv` |

Each financial operation requires an `Idempotency-Key` matching `[A-Za-z0-9._:-]{1,100}`. After a timeout, retry with the same key and same semantic request; the saved result is replayed. Reusing a key with a changed request returns a conflict. Failed requests do not reserve a successful key. See [docs/requests.http](docs/requests.http) for concrete requests.

## Financial and security boundaries

- Amounts use `BigDecimal`, must be positive, have at most two fractional digits, and up to 15 integer digits. Fractions of a cent are rejected; input is never silently rounded. PostgreSQL stores `NUMERIC(17,2)`.
- Only BRL is supported. Transfer account currencies must match.
- The authenticated owner comes from the verified JWT. Transfers may target another user's active account, but the response does not expose recipient owner data. Incoming postings appear in the recipient's history.
- An operation and its postings commit atomically. Transfers create one operation and two account postings. This is an internal account ledger, not a full double-entry general ledger with external clearing accounts.
- PostgreSQL-specific transaction-scoped advisory locks serialize matching idempotency keys. Row locks protect balances and account status during concurrent operations.
- Reports use UTC instants and half-open ranges `[from,to)`. CSV exports are limited to 10,000 rows and neutralize spreadsheet formulas in user-controlled text.
- JWT access tokens expire after 15 minutes. There are no refresh tokens, immediate token revocation, rate limits, MFA, or KYC/AML flows.
- Registration passwords must contain 12 to 72 UTF-8 bytes. The upper bound keeps BCrypt's input limit explicit; a character may use more than one byte.

## Design and learning

This modular monolith is organized around `auth`, `account`, `ledger`, `reporting`, and `shared`. Start with [architecture](docs/architecture.md), then follow the [learning chapters](docs/learning/01-java-fundamentals.md). Each chapter teaches Java through actual project classes and includes exactly three practical changes to implement and verify.

## Roadmap

Possible future work includes richer reconciliation and audit tooling, additional currencies with explicit conversion rules, operational monitoring, and production-grade identity and abuse controls. These require product and architecture decisions. No public deployment is claimed or configured here.
