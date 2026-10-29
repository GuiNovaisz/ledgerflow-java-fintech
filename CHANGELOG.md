# Changelog

All notable changes to LedgerFlow are documented here. This project follows the spirit of Keep a Changelog and uses Semantic Versioning.

## [0.1.0] - 2026-10-29

Initial portfolio release of LedgerFlow — Java Fintech Backend. Educational financial simulation, licensed under MIT.

### Added

- Educational modular-monolith service for simulated BRL accounts and ledger operations.
- JWT authentication, BCrypt password hashing, owner-scoped accounts, and account status management.
- Simulated deposits, withdrawals, and transfers with transactional balance updates, immutable postings, and idempotency keys.
- Paginated account history, UTC summaries, and formula-neutralized CSV statements.
- PostgreSQL persistence, Flyway migrations, Docker Compose setup, OpenAPI documentation, and learning materials.
- Unit and PostgreSQL integration test suites, with release verification documented separately.

### Fixed during release audit

- Preserve exact half-open report ranges when nanosecond API inputs meet PostgreSQL microsecond storage; verified by a failing-before/passing-after integration regression.
- Apply compatible security patches to Jackson, embedded Tomcat, PostgreSQL JDBC, Commons Lang, Log4j and test-scoped Commons Compress. Two remaining conditional Spring MVC advisories are explicitly assessed in the audit.
- Clarify setup requirements and protect existing local environment files in the documented setup commands.
- Replace deprecated GitHub Actions with maintained Node 24 versions, pinned by commit SHA, after warnings surfaced in the first remote CI run.

### Verified locally

- 38 automated tests passed with no failures, errors or skips; Docker Compose healthy, 10 real HTTP checks passed, and database balances reconciled. The first GitHub Actions run also passed Maven verification, image build and readiness checks.

See [v0.1.0 release notes](docs/releases/v0.1.0.md) and the [release audit](docs/release-audit.md) for scope and verification status.
