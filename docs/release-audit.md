# v0.1.0 release audit

Local release-candidate verification: **2026-10-28–29** (America/Sao_Paulo). Public repository creation, first push, tag and GitHub Release await the owner's explicit approval. This is an educational financial simulation, not a banking certification or a production-readiness claim.

## Findings and corrections

1. **Report date precision:** PostgreSQL stores microseconds, while API inputs accept nanoseconds. JDBC rounding could include a posting before `from` or exclude one before `to`. The new integration test failed on the old implementation (expected zero entries, received one). `ReportService` now ceilings both bounds to microseconds, preserving `[from,to)` across history, summary and CSV, including valid empty sub-microsecond intervals. The regression and full suite pass.
2. **Dependency advisories:** updated compatible patch releases through explicit Maven overrides. The baseline OSV scan had 22 advisory matches across eight artifacts; the current scan covers 141 unique Maven coordinates, including test dependencies, and retains two Spring MVC matches assessed below. The full resolved tree, rather than only declared direct dependencies, was checked.
3. **Publication privacy:** with the owner's explicit authorization, replaced the corporate author/committer email in all seven unpublished commits with the selected GitHub account's `noreply` address. Each commit's tree, message, name and dates were preserved, as were pending worktree/index changes. A private ignored Git bundle provides local recovery. No force push or remote history change occurred.
4. **Documentation:** clarified Docker-only setup versus host JDK requirements, protected existing `.env` files, added the approved MIT license, changelog, release notes and fresh evidence. Earlier verification records remain historical.

| Dependency family | Baseline | Candidate |
| --- | --- | --- |
| Jackson BOM (core/databind) | 2.21.4 | 2.21.8 |
| Embedded Tomcat | 10.1.55 | 10.1.61 |
| PostgreSQL JDBC | 42.7.11 | 42.7.14 |
| Apache Commons Lang | 3.17.0 | 3.21.0 |
| Log4j BOM/API | 2.24.3 | 2.25.5 |
| Apache Commons Compress (test dependency) | 1.24.0 | 1.28.0 |

Spring Boot remains 3.5.16 with Java 21. No major framework migration was introduced. Review overrides again when updating the Boot parent.

## Remaining dependency matches: applicability assessment

The [raw current scan](evidence/release-0.1.0/dependencies.json) retains both advisories; they are **not silently suppressed or claimed patched**. The OSV/GitHub records classify them as critical, while the vendor advisories classify them LOW and MEDIUM respectively. The assessment below is a source-review conclusion about this application's execution paths, not a change to upstream version ranges.

| Advisory on spring-webmvc 6.2.19 | Vendor severity | Disposition and evidence |
| --- | --- | --- |
| [CVE-2026-47890 / GHSA-j9f9-w8pj-32f8](https://spring.io/security/cve-2026-47890/) | LOW | `not_affected`: the application does not send view fragments using SSE. There are no `SseEmitter`, fragments or event-stream handlers in application code. |
| [CVE-2026-47884 / GHSA-pc63-qcmh-9cmg](https://spring.io/security/cve-2026-47884/) | MEDIUM | `not_affected`: no `XsltView` or XSLT view resolver is configured. Application controllers return REST bodies/CSV, with no implicit view rendering through a `/**` handler. Security URL matchers do not render views. |

The vendor lists 6.2.20 as Enterprise-only and 7.0.9 as an OSS fix. Retaining the tested Boot 3.5 stack is a scoped decision because the vulnerable features are absent. **Reassess before adding SSE view fragments, XSLT views, custom view resolvers or implicit view rendering**, and on every dependency upgrade. An additional read-only reviewer independently checked this assessment and the financial paths. No untreated applicable critical finding was identified within this audit's scope.

## Verification results

- **Maven: BUILD SUCCESS, 38 tests: 28 unit + 10 PostgreSQL integration; zero failures, errors or skipped tests.** Completed 2026-10-28 after the application and dependency changes; the saved successful reports were verified after the machine reboot. [Machine-readable test summary](evidence/release-0.1.0/tests.json).
- The integration suite exercises JWT/login and owner isolation, decimals, retries, concurrent spending, opposite-direction transfers, immutable postings, rollback, reporting and OpenAPI contracts. Testcontainers used PostgreSQL 16.15 and applied Flyway V1 to a fresh database.
- Docker Compose was rebuilt and started successfully on 2026-10-29. Application and PostgreSQL reached `healthy`; runtime user is UID/GID 10001, and published ports bind to loopback.
- The real HTTP smoke test passed **10 checks**, covering health, OpenAPI, login/registration, deposit, cross-user transfer, idempotent replay, withdrawal, authorization, summary, CSV, incoming history and inactive-account rejection. [Sanitized HTTP evidence](evidence/release-0.1.0/http-smoke.json).
- Flyway V1 is successful in Compose. Read-only SQL found **zero unreconciled account balances and zero unbalanced transfers**. [Database evidence](evidence/release-0.1.0/database-check.json). The HTTP script adds synthetic data and never clears existing data.

Environment: Windows / PowerShell 7, Temurin 21.0.12.1+1, Maven Wrapper 3.9.9, Spring Boot 3.5.16, PostgreSQL 16.15. The portable host JDK lives under ignored `.tools`; only process-local Java variables were set. No global tool installation or Windows security setting change was needed.

Reproduction commands (select a JDK 21 before Maven):

```powershell
.\mvnw.cmd -B -ntp -Pintegration verify
.\mvnw.cmd -B -ntp dependency:tree -DoutputType=json -DoutputFile=target/dependencies.json
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 120
.\scripts\Verify-Api.ps1 -OutputPath target/http-smoke.json
docker compose ps
docker compose exec -T app id
```

For the dependency scan, enumerate unique `groupId:artifactId` plus version from the resolved JSON tree and submit `{package:{ecosystem:"Maven",name:"group:artifact"},version:"..."}` entries to `https://api.osv.dev/v1/querybatch`. Retain all results, check response count and pagination, and assess matching advisories against the actual application. The recorded scan includes its UTC query time. Build plugins, OS packages and base-image contents are outside this Maven dependency scan.

## Secrets, Git and publication preparation

Gitleaks 8.30.1 was downloaded from the official release and checked against its SHA-256. The baseline history scan reported seven occurrences of one fixed synthetic JWT subject UUID in a unit test, not a credential. `.gitleaks.toml` extends the default rules and allows only that exact UUID in that exact file; signing keys remain generated per test. The reviewed seven-commit history scan found zero leaks. The final candidate snapshot and reachable history are also scanned before handoff; see [secret scan evidence](evidence/release-0.1.0/secrets.json).

Ignored `.env`, `.tools`, build outputs and logs are not part of the publication candidate. No operational password, token or signing key was found in the reviewed publishable content. The local recovery bundle is ignored and must stay private. Secret scanning is heuristic, not proof of absence.

Prepared destination: `GuiNovaisz/ledgerflow`, public, branch `main`. There is no remote configured yet and no tag or release created. Repository description: “Educational financial ledger REST API built with Java 21, Spring Boot, PostgreSQL and Docker, with transactional consistency, idempotency, JWT authentication and automated tests.” Suggested topics: `java`, `spring-boot`, `backend`, `postgresql`, `docker`, `rest-api`, `testcontainers`, `portfolio`.

After explicit publication approval: create the repository, push `main`, verify remote SHA and GitHub Actions, then create annotated tag `v0.1.0` on the approved commit and publish [the prepared release notes](releases/v0.1.0.md). Do not force-push or overwrite an existing tag. GitHub Actions has not yet run remotely.

## Boundaries and next priorities

This is a source review plus local correctness testing, not a penetration test, performance benchmark or compliance assessment. No real money moves. The database role is shared between migration/runtime, image tags are mutable, and access tokens have no immediate revocation. Public production deployment needs separate operational work.

Priority improvements: (1) reconciliation tooling and broader invariant/property testing, (2) rate limiting and identity lifecycle controls, (3) production observability, least-privilege database roles, secret management and pinned image supply-chain checks.

For a portfolio discussion, be ready to explain why locks are acquired in UUID order, how owner-scoped idempotency handles concurrent retries and rollback, how balance/posting consistency is enforced, and why `[from,to)` needs ceiling conversion for microsecond storage.
