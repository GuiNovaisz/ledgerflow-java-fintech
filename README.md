# LedgerFlow — Java Fintech Backend

[![CI](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/workflows/ci.yml)

LedgerFlow is an educational fintech simulation for exploring how a small account ledger handles money-like operations safely and consistently. It addresses common backend concerns—identity, account ownership, concurrent balance changes, retries, and audit history—in a runnable project. Deposits and withdrawals are simulated; no real funds move.

> **Simulation only.** LedgerFlow does not connect to banks, cards, payment providers, settlement rails, or real money. It is not a production banking platform.

[Português (Brasil)](README.pt-BR.md) · [Architecture](docs/architecture.md) · [Diagrams](docs/diagrams.md) · [API examples](docs/requests.http) · [Learning path](docs/learning/01-java-fundamentals.md) · [Verification evidence](docs/verification.md)

## Features

- Stateless bearer JWT authentication and BCrypt password hashing.
- Owner-scoped BRL accounts with activation and deactivation.
- Simulated deposits, withdrawals, and transfers recorded as immutable ledger postings.
- PostgreSQL transactions, row locks, and idempotency keys for financial requests.
- Paginated history, UTC date-range summaries, and CSV statements with formula neutralization.
- Flyway-managed schema, Docker Compose environment, and OpenAPI documentation.

## Technical decisions

The project is a modular monolith: one Spring Boot process and one PostgreSQL database, organized by capability (`auth`, `account`, `ledger`, `reporting`, `shared`). Money uses `BigDecimal` and fixed-scale `NUMERIC`, avoiding floating-point arithmetic. Each financial operation updates balances and records postings in one database transaction. Row locks serialize balance changes; transaction-scoped PostgreSQL advisory locks coordinate matching idempotency keys. These choices keep the example understandable while making its consistency rules explicit. See [architecture](docs/architecture.md) for invariants and tradeoffs.

## Stack

Java 21, Spring Boot 3.5, Maven Wrapper, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, Testcontainers, Docker Compose, and springdoc OpenAPI.

## Requirements

For host builds and tests: JDK 21 and network access for Maven dependencies. For the full local service or PostgreSQL integration profile: Docker Engine with Compose (or Docker Desktop on Windows). The wrapper supplies Maven. Docker Compose builds the app with JDK 21 and runs it on a Java 21 runtime image.

## Start locally

Clone the public repository, then follow the instructions for your shell:

```sh
git clone https://github.com/GuiNovaisz/ledgerflow-java-fintech.git
cd ledgerflow-java-fintech
```

### Linux / macOS shell

```sh
chmod +x mvnw
# Creates random local credentials; noclobber prevents replacing an existing .env.
(set -C; umask 077; printf 'POSTGRES_PASSWORD=%s\nJWT_SECRET=%s\nAPP_PORT=8080\n' "$(openssl rand -base64 36 | tr -d '\n')" "$(openssl rand -base64 48 | tr -d '\n')" > .env)
docker compose up --build -d --wait
```

### Windows PowerShell

```powershell
.\scripts\New-LocalEnvironment.ps1
docker compose up --build -d --wait
```

The environment script creates `.env` with random secrets and refuses to overwrite an existing path. `.env` is ignored by Git. These Docker Compose steps do not require a host JDK; Compose builds the application with JDK 21. Compose binds PostgreSQL and the application to loopback by default; set `APP_PORT` in `.env` to change the application port. Inside Compose, the database is `postgres:5432`.

To build or run tests on the host, install JDK 21 and use the Maven Wrapper. A Docker-compatible engine is required for the PostgreSQL integration profile, but not for unit tests. For example, run `./mvnw test` on Linux/macOS or `.\mvnw.cmd test` on PowerShell.

OpenAPI UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

To stop containers and preserve the local database volume:

```sh
docker compose down
```

## Tests and release audit

Run unit tests with `./mvnw test` (Linux/macOS) or `.\mvnw.cmd test` (PowerShell). The PostgreSQL integration profile is `./mvnw -Pintegration verify` or `.\mvnw.cmd -Pintegration verify`; it requires JDK 21 and a working Docker-compatible engine and must not be described as verified if skipped or unavailable.

The [v0.1.0 release audit](docs/release-audit.md) records **38 passing tests (28 unit + 10 PostgreSQL integration), zero skipped**, a healthy Compose environment and 10 passing local HTTP checks. The [first GitHub Actions run](https://github.com/GuiNovaisz/ledgerflow-java-fintech/actions/runs/40873784507) also passed the Maven suite, Docker image build and readiness check. Dependency patch updates and the applicability assessment for two remaining Spring advisories are documented in the audit. The [earlier 37-test record](docs/verification.md) is retained as historical evidence.

After Compose is running, the PowerShell 7 API smoke script can be run with `./scripts/Verify-Api.ps1` (PowerShell) and creates synthetic data without clearing the database.

## API overview

Public endpoints: `POST /api/v1/auth/register` and `POST /api/v1/auth/login`. Other `/api/**` routes require `Authorization: Bearer <accessToken>`.

| Capability | Routes |
| --- | --- |
| Accounts | `POST /api/v1/accounts`, `GET /api/v1/accounts`, `GET /api/v1/accounts/{id}`, `PATCH /api/v1/accounts/{id}/status` |
| Financial operations | `POST /api/v1/transactions/deposits`, `/withdrawals`, `/transfers` |
| Account reporting | `GET /api/v1/accounts/{id}/entries`, `/summary`, `/statement.csv` |

Financial operations require an `Idempotency-Key` matching `[A-Za-z0-9._:-]{1,100}`. Retry a timed-out request with the same key and same semantic request; changing the request under an existing key returns a conflict. See [API examples](docs/requests.http).

## Known limitations and security boundaries

- BRL only; amounts must be positive, have at most two fractional digits, and fit the supported precision. Fractions of a cent are rejected rather than rounded.
- Transfers may credit another user's active account, but recipient owner data is not exposed. This is an internal account ledger, not a full double-entry general ledger with external clearing accounts.
- Reports use UTC instants and half-open date ranges `[from,to)`. CSV exports are capped at 10,000 rows and neutralize spreadsheet formulas in user-controlled text.
- Access tokens expire after 15 minutes. There are no refresh tokens, immediate token revocation, rate limits, MFA, or KYC/AML flows.
- This project does not provide external reconciliation, currency conversion, fees, interest, production operations, compliance, or a public deployment.

## Next steps

Potential follow-up work includes reconciliation and audit tooling, operational monitoring, and identity and abuse controls suitable for a real service. Those require separate product and architecture decisions.

## License and release notes

[MIT License](LICENSE) · [Changelog](CHANGELOG.md) · [v0.1.0 release notes](docs/releases/v0.1.0.md) · [GitHub Release](https://github.com/GuiNovaisz/ledgerflow-java-fintech/releases/tag/v0.1.0)
