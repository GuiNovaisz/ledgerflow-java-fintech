# 5. Security and ownership

Security begins with explicit identity and data boundaries. LedgerFlow uses Spring Security's JWT resource server support, BCrypt, owner-scoped service calls, and generic not-found behavior for foreign account IDs.

In the account service, authorization uses the authenticated owner's ID in the database lookup:

```java
public Account requireOwned(UUID owner, UUID id) {
    return accounts.findByIdAndOwnerId(id, owner).orElseThrow(AccountService::notFound);
}
```

The repository query matches both the account ID and owner ID. A random UUID by itself is not proof that the caller may access the account.

## Authentication is not authorization

Authentication answers “who is making the request?” Registration stores a BCrypt hash; login checks the password and issues a signed JWT with a 15-minute lifetime. Spring Security validates a bearer token before protected routes run. The controller reads the authenticated subject from that token.

Authorization answers “what may this user do?” The service uses the verified subject as the owner. A JSON request cannot set the owner. For owner-only account reads and source-account mutations, an account belonging to someone else returns the same not-found behavior as an unknown account, limiting identifier probing.

## Recipient privacy

Transfers may credit any existing active account, including another user's. The recipient UUID is already part of the caller's request and may appear in the operation response. The service does not return recipient name, email, other accounts, or resulting balance. Reporting is scoped to accounts owned by the authenticated user, so recipients can see their incoming credit.

## Secrets and token limits

`New-LocalEnvironment.ps1` creates random local secrets without overwriting an existing `.env`. Never commit real secrets. Compose passes environment values to containers; a standalone app needs those variables exported in its process environment. Access tokens expire, but this version has no refresh flow or immediate revocation. It also lacks rate limiting, MFA, and KYC/AML processes.

## Safe exports and errors

CSV text can be interpreted as a spreadsheet formula when it begins with certain characters. The exporter neutralizes formula-like user text and escapes CSV delimiters. API error responses use stable public messages and do not include stack traces, SQL, passwords, JWTs, or signing secrets.

## Practical changes (exactly three)

1. Extend `AuthServiceTest` with the registration UTF-8 upper boundary: accept 36 `é` characters (72 bytes) and reject `a` plus 36 `é` characters (73 bytes). Existing tests cover 12 bytes and a 76-byte rejection, but not this boundary. Verify with `./mvnw.cmd -Dtest=AuthServiceTest test`.
2. Extend `protocolErrorsPreserveHttpStatusAndDoNotBecomeServerErrors` in `LedgerFlowIT` to assert the invalid-bearer response has the generic detail `Authentication is required` and does not echo `invalid-token`. The current test checks only `401`. Verify with `./mvnw.cmd -Pintegration verify` and Docker available.
3. Extend the registration integration test to assert the response contains neither `password` nor `passwordHash`. The current registration test checks IDs and email but does not explicitly assert those secrets are absent. Verify with `./mvnw.cmd -Pintegration verify`.

## Interview check

**Why is a valid JWT not enough to authorize an account operation?** The token establishes identity; the application must still check the requested account belongs to that identity.

**Why return not found for a foreign account?** It avoids confirming whether an account identifier exists for another user on owner-only endpoints.

**What does password hashing provide?** A one-way password verifier makes a database leak less directly useful than storing plaintext. BCrypt is intentionally costly, but it does not replace secret handling or login abuse controls.
