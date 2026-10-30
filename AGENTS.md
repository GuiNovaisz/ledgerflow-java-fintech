# LedgerFlow — repository instructions

- Use `DEV` for ongoing development, as explicitly requested by the owner. Keep `main` for reviewed, stable releases.
- Before editing, inspect the current branch and working tree. Preserve existing user changes. Do not reset or overwrite branches to move work.
- Optional task branches use the `codex/` prefix and integrate into `DEV`. Do not spawn agents unless explicitly authorized by the owner or applicable instructions.
- Run checks appropriate to the change. Financial/authentication changes require PostgreSQL integration tests; browser workflows require meaningful frontend/E2E checks.
- Use JDK 21 with process-local JAVA_HOME; use the Maven wrapper. Prefer background CLI/API work and isolated headless browser tests.
- Keep `.env`, `.local/`, credentials, tokens and raw private artifacts out of Git and Docker build contexts.
- Never force push or move a published release tag. Preserve v0.1.0 and subsequent published milestones.
- Publishing a release requires owner authorization for that release. The v0.2.0 publication was explicitly authorized on 2026-10-30; this does not authorize future releases automatically.
- After release work, return to `DEV`. Keep requested local services running and preserve their volumes.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch and verification workflow.
