# 3. Spring Boot request flow

Spring Boot starts the application, discovers configured components, builds their dependencies, and hosts the HTTP server. In this project, `LedgerFlowApplication` is the entry point. Annotations tell Spring how to connect the code; they do not remove the need to understand the method calls.

This is the existing account creation route from `AccountController`:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public AccountView create(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateAccount request) {
    return accounts.create(UUID.fromString(jwt.getSubject()), request.name());
}
```

`@PostMapping` maps an HTTP POST to this method. Spring converts JSON into `CreateAccount`, runs its validation because of `@Valid`, and supplies the already verified token. The method passes the token's subject and request name to the service; it does not take an owner ID from JSON.

## From HTTP to service

`@RestController` marks a class whose methods serve HTTP. `@RequestMapping` sets a shared path prefix, and `@PostMapping` maps a method to a route. `@RequestBody` converts JSON into a DTO; `@Valid` runs Jakarta validation annotations such as `@NotNull` and `@Digits`. `@AuthenticationPrincipal Jwt` gives the controller the JWT already verified by Spring Security.

Spring injects the service via constructor. The service calls repositories and the database. The response record is serialized back to JSON. `ApiExceptionHandler` turns expected exceptions into HTTP Problem Details.

## Dependency injection

`@Service` makes `TransactionService` a Spring-managed component. Constructor parameters define what it needs. Spring finds the matching repository, `JdbcTemplate`, and JSON mapper and supplies them when creating the service. This is dependency injection; it avoids manually constructing the whole object graph in application code.

## Configuration and migrations

`application.yml` contains defaults and references environment variables such as `DB_URL` and `JWT_SECRET`. Flyway applies versioned SQL from `src/main/resources/db/migration`. Hibernate is configured to validate the schema, not create it automatically. This makes the SQL migration the explicit schema history.

The Compose file loads its `.env` for containers. Spring Boot itself does not automatically parse that file when launched outside Compose. Export variables in the shell that launches the app.

## Transactions

`@Transactional` on `TransactionService.execute` defines a database transaction around the use case. The method changes locked accounts and saves the operation and entries; a thrown error causes rollback. The HTTP success is returned only after the Spring transaction interceptor completes successfully. A network call should not be added inside this transaction because it can hold database locks while waiting on another service.

## Practical changes (exactly three)

1. Add an integration test that sends an account JSON body with an unknown `ownerId` property and expects `400`; Jackson is configured to reject unknown properties, so the caller cannot silently supply an owner. Verify with `./mvnw.cmd -Pintegration verify` and Docker available.
2. Add an integration test for `GET /api/v1/accounts/not-a-uuid` with a valid token and assert `400`; this exercises Spring's path conversion and the shared error handler. Verify with `./mvnw.cmd -Pintegration verify`.
3. Add a short `@Operation` summary to `AccountController.create`, then open `/v3/api-docs` or Swagger UI and confirm the route has the summary. This exercises how annotations feed the generated API docs without changing endpoint behavior.

## Interview check

**What is dependency injection?** A container supplies a component's dependencies, usually through its constructor, instead of the component creating them itself.

**What does `@Transactional` guarantee here?** Database writes in the proxied method commit together or roll back together on failure; it does not make an external network service part of the same PostgreSQL transaction.

**Why use Flyway if Hibernate knows the entities?** Versioned SQL migrations make schema changes ordered and reviewable, while Hibernate's `validate` mode checks mappings against the resulting schema.
