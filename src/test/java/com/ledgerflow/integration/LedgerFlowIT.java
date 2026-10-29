package com.ledgerflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

/** End-to-end contract and transactional checks against PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class LedgerFlowIT {
    private static final boolean EXTERNAL_DATABASE = System.getProperty("test.database.url") != null;
    private static final String TEST_JWT_SECRET = randomJwtSecret();

    private static final PostgreSQLContainer<?> POSTGRES = EXTERNAL_DATABASE ? null
            : new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        if (EXTERNAL_DATABASE) {
            properties.add("spring.datasource.url", () -> System.getProperty("test.database.url"));
            properties.add("spring.datasource.username", () -> System.getProperty("test.database.username", "postgres"));
            properties.add("spring.datasource.password", () -> System.getProperty("test.database.password", ""));
        } else {
            if (!POSTGRES.isRunning()) POSTGRES.start();
            properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            properties.add("spring.datasource.username", POSTGRES::getUsername);
            properties.add("spring.datasource.password", POSTGRES::getPassword);
        }
        properties.add("ledgerflow.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private record User(String token, String id, String email) {}
    private record Account(String id, String token) {}

    @BeforeAll
    void requirePostgres16ForFallback() {
        if (EXTERNAL_DATABASE) {
            String version = jdbc.queryForObject("select current_setting('server_version_num')", String.class);
            assertThat(Integer.parseInt(version)).isBetween(160000, 169999);
        }
    }

    @AfterAll
    static void stopContainer() {
        if (POSTGRES != null && POSTGRES.isRunning()) POSTGRES.stop();
    }

    @Test
    void completeFinancialFlowAndRecipientCanSeeTheirPosting() throws Exception {
        User sender = user();
        User recipient = user();
        Account source = account(sender, "Main");
        Account target = account(recipient, "Recipient");
        deposit(sender, source, "100.00", "seed-key", "FOOD", "seed income");
        withdraw(sender, source, "20.00", "food-expense", "FOOD", "groceries");
        withdraw(sender, source, "5.00", "transport-expense", "TRANSPORT", "bus");

        JsonNode transfer = transfer(sender, source, target, "35.25", "transfer-key", 201);
        assertThat(transfer.path("type").asText()).isEqualTo("TRANSFER");
        assertThat(transfer.path("sourceAccountId").asText()).isEqualTo(source.id());
        assertThat(transfer.path("targetAccountId").asText()).isEqualTo(target.id());
        assertThat(balance(sender, source)).isEqualByComparingTo("39.75");
        assertThat(balance(recipient, target)).isEqualByComparingTo("35.25");
        JsonNode entries = getJson(recipient, "/api/v1/accounts/" + target.id() + "/entries?size=20&page=0");
        assertThat(entries.path("content").get(0).path("direction").asText()).isEqualTo("CREDIT");
        assertThat(entries.path("content").get(0).path("operationId").asText()).isEqualTo(transfer.path("id").asText());
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries where operation_id = ?", Integer.class,
                UUID.fromString(transfer.path("id").asText()))).isEqualTo(2);
        assertThat(transfer.has("recipientOwner")).isFalse();

        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2035-01-01T00:00:00Z");
        String base = "/api/v1/accounts/" + source.id();
        JsonNode summary = getJson(sender, base + "/summary?from=" + from + "&to=" + to);
        assertThat(summary.path("income").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(summary.path("expenses").decimalValue()).isEqualByComparingTo("60.25");
        assertThat(summary.path("net").decimalValue()).isEqualByComparingTo("39.75");
        assertThat(summary.path("expensesByCategory").path("FOOD").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(summary.path("expensesByCategory").path("TRANSPORT").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(summary.path("expensesByCategory").path("OTHER").decimalValue()).isEqualByComparingTo("35.25");

        JsonNode firstPage = getJson(sender, base + "/entries?from=" + from + "&to=" + to + "&page=0&size=2");
        assertThat(firstPage.path("content").size()).isEqualTo(2);
        assertThat(firstPage.path("page").asInt()).isZero();
        assertThat(firstPage.path("size").asInt()).isEqualTo(2);
        assertThat(firstPage.path("totalElements").asLong()).isEqualTo(4);
        assertThat(firstPage.path("totalPages").asInt()).isEqualTo(2);
        JsonNode secondPage = getJson(sender, base + "/entries?from=" + from + "&to=" + to + "&page=1&size=2");
        assertThat(secondPage.path("content").size()).isEqualTo(2);
        assertThat(firstPage.path("content").get(0).path("id").asText())
                .isNotEqualTo(firstPage.path("content").get(1).path("id").asText());
        assertThat(firstPage.path("content").get(0).path("createdAt").asText())
                .isGreaterThanOrEqualTo(firstPage.path("content").get(1).path("createdAt").asText());

        MvcResult latestResult = mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(sender))
                        .header("Idempotency-Key", "date-boundary").contentType(MediaType.APPLICATION_JSON)
                        .content(cashBody(source, "3.00", "OTHER", "exclusive-boundary")))
                .andExpect(status().isCreated()).andReturn();
        UUID latestOperation = UUID.fromString(json.readTree(latestResult.getResponse().getContentAsString()).path("id").asText());
        Instant exactCreatedAt = jdbc.queryForObject("select created_at from ledger_entries where operation_id=?",
                java.sql.Timestamp.class, latestOperation).toInstant();
        String exact = exactCreatedAt.toString();
        JsonNode inclusive = getJson(sender, base + "/entries?from=" + exact + "&to=" + exactCreatedAt.plusNanos(1_000) + "&page=0&size=20");
        assertThat(inclusive.path("totalElements").asLong()).isEqualTo(1);
        JsonNode exclusive = getJson(sender, base + "/entries?from=2020-01-01T00:00:00Z&to=" + exact + "&page=0&size=20");
        assertThat(exclusive.path("content").findValuesAsText("operationId")).doesNotContain(latestOperation.toString());
        MvcResult csv = mvc.perform(get("/api/v1/accounts/" + source.id() + "/statement.csv")
                        .param("from", from.toString()).param("to", to.toString())
                        .header("Authorization", bearer(sender)))
                .andExpect(status().isOk()).andReturn();
        assertThat(csv.getResponse().getContentType()).contains("text/csv");
        assertThat(csv.getResponse().getContentAsString()).contains("id,operationId,type,direction,amount,currency,category,description,createdAt", "FOOD", "TRANSPORT", "OTHER");
    }

    @Test
    void reportTimeFiltersRespectNanosecondHalfOpenBoundaries() throws Exception {
        User owner = user();
        Account account = account(owner, "Report time boundaries");
        JsonNode operation = deposit(owner, account, "17.25", "report-time-boundary");
        UUID operationId = UUID.fromString(operation.path("id").asText());
        Instant createdAt = Instant.parse(operation.path("createdAt").asText());
        String base = "/api/v1/accounts/" + account.id();

        Instant after = createdAt.plusNanos(1);
        Instant afterEnd = createdAt.plusSeconds(1);
        JsonNode excludedEntries = getJson(owner, base + "/entries?from=" + after + "&to=" + afterEnd + "&page=0&size=20");
        assertThat(excludedEntries.path("totalElements").asLong()).isZero();
        JsonNode excludedSummary = getJson(owner, base + "/summary?from=" + after + "&to=" + afterEnd);
        assertThat(excludedSummary.path("income").decimalValue()).isEqualByComparingTo("0.00");
        MvcResult excludedCsv = mvc.perform(get(base + "/statement.csv").param("from", after.toString())
                        .param("to", afterEnd.toString()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andReturn();
        assertThat(excludedCsv.getResponse().getContentAsString()).doesNotContain(operationId.toString());

        Instant before = createdAt.minusSeconds(1);
        Instant through = createdAt.plusNanos(1);
        JsonNode includedEntries = getJson(owner, base + "/entries?from=" + before + "&to=" + through + "&page=0&size=20");
        assertThat(includedEntries.path("totalElements").asLong()).isEqualTo(1);
        assertThat(includedEntries.path("content").get(0).path("operationId").asText()).isEqualTo(operationId.toString());
        JsonNode includedSummary = getJson(owner, base + "/summary?from=" + before + "&to=" + through);
        assertThat(includedSummary.path("income").decimalValue()).isEqualByComparingTo("17.25");
        MvcResult includedCsv = mvc.perform(get(base + "/statement.csv").param("from", before.toString())
                        .param("to", through.toString()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andReturn();
        assertThat(includedCsv.getResponse().getContentAsString()).contains(operationId.toString());

        JsonNode tinyInterval = getJson(owner, base + "/entries?from=" + after + "&to=" + createdAt.plusNanos(2)
                + "&page=0&size=20");
        assertThat(tinyInterval.path("totalElements").asLong()).isZero();
    }

    @Test
    void authenticationValidationAndOwnershipAreEnforced() throws Exception {
        MvcResult registration = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email() + "\",\"password\":\"password-strong-123\",\"displayName\":\"Test User\"}"))
                .andExpect(status().isCreated()).andReturn();
        JsonNode user = json.readTree(registration.getResponse().getContentAsString());
        assertThat(user.path("id").asText()).isNotBlank();
        assertThat(user.path("email").asText()).isNotBlank();
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.path("email").asText() + "\",\"password\":\"password-strong-123\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(login.getResponse().getContentAsString());
        assertThat(token.path("accessToken").asText()).isNotBlank();
        assertThat(token.path("tokenType").asText()).isEqualToIgnoringCase("Bearer");

        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
        User owner = user(); User stranger = user();
        Account account = account(owner, "Private");
        mvc.perform(get("/api/v1/accounts/" + account.id()).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(stranger))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + account.id() + "\",\"amount\":10,\"category\":\"OTHER\",\"description\":\"x\"}"))
                .andExpect(status().isNotFound());
        Account strangerAccount = account(stranger, "Stranger target");
        String foreignTransfer = "{\"sourceAccountId\":\"" + account.id() + "\",\"targetAccountId\":\"" + strangerAccount.id() + "\",\"amount\":1,\"category\":\"OTHER\",\"description\":\"x\"}";
        mvc.perform(post("/api/v1/transactions/withdrawals").header("Authorization", bearer(stranger))
                        .header("Idempotency-Key", "foreign-withdrawal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + account.id() + "\",\"amount\":1,\"category\":\"OTHER\",\"description\":\"x\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/transactions/transfers").header("Authorization", bearer(stranger))
                        .header("Idempotency-Key", "foreign-transfer").contentType(MediaType.APPLICATION_JSON)
                        .content(foreignTransfer))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/accounts/" + account.id() + "/status").header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/" + account.id() + "/entries").header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/" + account.id() + "/summary").header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/" + account.id() + "/statement.csv").header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/accounts").header("Authorization", bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idempotentReplayConflictAndConcurrentRetries() throws Exception {
        User u = user(); Account a = account(u, "Idempotency");
        deposit(u, a, "50.00", "once");
        JsonNode replay = deposit(u, a, "50.00", "once");
        assertThat(replay.path("id").asText()).isEqualTo(jdbc.queryForObject(
                "select id::text from operations where owner_id=? and idempotency_key='once'", String.class, UUID.fromString(u.id())));
        deposit(u, a, "51.00", "once", 409);

        String key = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<String>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) calls.add(pool.submit(() -> {
                ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return deposit(u, a, "7.00", key).path("id").asText();
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(calls.get(0).get(30, TimeUnit.SECONDS)).isEqualTo(calls.get(1).get(30, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertThat(jdbc.queryForObject("select count(*) from operations where owner_id=? and idempotency_key=?", Integer.class, UUID.fromString(u.id()), key)).isEqualTo(1);
    }

    @Test
    void concurrentWithdrawalsCannotOverspend() throws Exception {
        User u = user(); Account a = account(u, "Concurrent"); deposit(u, a, "100.00", "fund");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final String key = "withdraw-" + i;
                calls.add(pool.submit(() -> {
                    ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                    return mvc.perform(post("/api/v1/transactions/withdrawals").header("Authorization", bearer(u))
                                    .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"accountId\":\"" + a.id() + "\",\"amount\":80,\"category\":\"OTHER\",\"description\":\"race\"}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            List<Integer> statuses = List.of(calls.get(0).get(30, TimeUnit.SECONDS), calls.get(1).get(30, TimeUnit.SECONDS));
            assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
            assertThat(statuses.stream().filter(s -> s == 409).count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
        assertThat(balance(u, a)).isEqualByComparingTo("20.00");
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries where account_id=?", Integer.class,
                UUID.fromString(a.id()))).isEqualTo(2);
    }

    @Test
    void invalidMoneyAndIdempotencyKeysAreRejectedAndInactiveReplayStillWorks() throws Exception {
        User u = user(); Account a = account(u, "Input validation");
        JsonNode committed = deposit(u, a, "12.00", "replay-after-deactivation");
        mvc.perform(patch("/api/v1/accounts/" + a.id() + "/status").header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        JsonNode replay = deposit(u, a, "12.00", "replay-after-deactivation");
        assertThat(replay.path("id").asText()).isEqualTo(committed.path("id").asText());
        assertThat(balance(u, a)).isEqualByComparingTo("12.00");

        mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "   ").contentType(MediaType.APPLICATION_JSON)
                        .content(cashBody(a, "1.00", "OTHER", "blank key")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON).content(cashBody(a, "1.00", "OTHER", "missing key")))
                .andExpect(status().isBadRequest());
        int index = 0;
        for (String amount : List.of("0", "-1", "0.001")) {
            mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(u))
                            .header("Idempotency-Key", "invalid-amount-" + index++).contentType(MediaType.APPLICATION_JSON)
                            .content(cashBody(a, amount, "OTHER", "invalid amount")))
                    .andExpect(status().isBadRequest());
        }
        assertThat(balance(u, a)).isEqualByComparingTo("12.00");
    }

    @Test
    void insufficientTransferRollsBackWithoutReservingItsKey() throws Exception {
        User u = user(); Account source = account(u, "Limited source"), target = account(u, "Empty target");
        deposit(u, source, "10.00", "limited-funds");
        String key = "insufficient-transfer";
        transfer(u, source, target, "11.00", key, 409);
        assertThat(balance(u, source)).isEqualByComparingTo("10.00");
        assertThat(balance(u, target)).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("select count(*) from operations where owner_id=? and idempotency_key=?", Integer.class, UUID.fromString(u.id()), key)).isZero();
        JsonNode successfulRetry = transfer(u, source, target, "5.00", key, 201);
        assertThat(successfulRetry.path("id").asText()).isNotBlank();
        assertThat(balance(u, source)).isEqualByComparingTo("5.00");
        assertThat(balance(u, target)).isEqualByComparingTo("5.00");
    }

    @Test
    void statusRulesSameAccountAndOppositeTransfers() throws Exception {
        User u = user(); Account a = account(u, "A"), b = account(u, "B");
        deposit(u, a, "100.00", "a-fund"); deposit(u, b, "100.00", "b-fund");
        mvc.perform(patch("/api/v1/accounts/" + a.id() + "/status").header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        deposit(u, a, "1", "inactive", 409);
        withdraw(u, a, "1", "inactive-withdraw", "OTHER", "inactive", 409);
        mvc.perform(post("/api/v1/transactions/transfers").header("Authorization", bearer(u))
                        .header("Idempotency-Key", "same-end").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":\"" + b.id() + "\",\"targetAccountId\":\"" + b.id() + "\",\"amount\":1,\"category\":\"OTHER\",\"description\":\"bad\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/v1/accounts/" + a.id() + "/status").header("Authorization", bearer(u))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            Future<Integer> ab = pool.submit(() -> concurrentTransfer(u, a, b, "10", "ab", ready, start));
            Future<Integer> ba = pool.submit(() -> concurrentTransfer(u, b, a, "10", "ba", ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(ab.get(30, TimeUnit.SECONDS)).isEqualTo(201); assertThat(ba.get(30, TimeUnit.SECONDS)).isEqualTo(201);
        } finally { pool.shutdownNow(); }
        assertThat(balance(u, a).add(balance(u, b))).isEqualByComparingTo("200.00");
    }

    @Test
    void databaseRollbackAndLedgerRowsAreImmutable() throws Exception {
        User u = user(); Account source = account(u, "Rollback source"), target = account(u, "Rollback target");
        deposit(u, source, "80.00", "rollback-fund");
        String trigger = "reject_credit_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE FUNCTION " + trigger + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN " +
                "IF NEW.account_id = '" + target.id() + "'::uuid AND NEW.direction = 'CREDIT' THEN RAISE EXCEPTION 'forced integration rollback'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER " + trigger + " BEFORE INSERT ON ledger_entries FOR EACH ROW EXECUTE FUNCTION " + trigger + "()");
        String key = UUID.randomUUID().toString();
        try {
            transfer(u, source, target, "20.00", key, 500);
            assertThat(balance(u, source)).isEqualByComparingTo("80.00");
            assertThat(jdbc.queryForObject("select count(*) from operations where owner_id=? and idempotency_key=?", Integer.class, UUID.fromString(u.id()), key)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS " + trigger + " ON ledger_entries");
            jdbc.execute("DROP FUNCTION IF EXISTS " + trigger + "()");
        }
        UUID operationId = UUID.fromString(deposit(u, source, "1.00", "immutable-probe").path("id").asText());
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> jdbc.update(
                "update ledger_entries set amount=amount where operation_id=?", operationId))).isNotNull();
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> jdbc.update(
                "delete from ledger_entries where operation_id=?", operationId))).isNotNull();
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> jdbc.update(
                "update operations set description=description where id=?", operationId))).isNotNull();
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> jdbc.update(
                "delete from operations where id=?", operationId))).isNotNull();
    }

    private int concurrentTransfer(User u, Account from, Account to, String amount, String key,
                                   CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        return mvc.perform(post("/api/v1/transactions/transfers").header("Authorization", bearer(u))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":\"" + from.id() + "\",\"targetAccountId\":\"" + to.id() + "\",\"amount\":" + amount + ",\"category\":\"OTHER\",\"description\":\"parallel\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void protocolErrorsPreserveHttpStatusAndDoNotBecomeServerErrors() throws Exception {
        User u = user();
        mvc.perform(delete("/api/v1/accounts").header("Authorization", bearer(u)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Allow"));
        mvc.perform(post("/api/v1/accounts").header("Authorization", bearer(u))
                .contentType(MediaType.TEXT_PLAIN).content("not-json"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        MvcResult docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode schema = json.readTree(docs.getResponse().getContentAsString());
        assertThat(schema.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(schema.path("paths").path("/api/v1/auth/login").path("post").path("security").isEmpty()).isTrue();
    }

    private User user() throws Exception {
        String email = email();
        MvcResult registered = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-strong-123\",\"displayName\":\"Integration User\"}"))
                .andExpect(status().isCreated()).andReturn();
        MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password-strong-123\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode user = json.readTree(registered.getResponse().getContentAsString());
        return new User(json.readTree(login.getResponse().getContentAsString()).path("accessToken").asText(), user.path("id").asText(), email);
    }

    private Account account(User u, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/accounts").header("Authorization", bearer(u))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("name", name))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode account = json.readTree(result.getResponse().getContentAsString());
        assertThat(account.path("currency").asText()).isEqualTo("BRL");
        assertThat(account.path("balance").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.path("status").asText()).isEqualTo("ACTIVE");
        return new Account(account.path("id").asText(), u.token());
    }

    private JsonNode deposit(User u, Account a, String amount, String key) throws Exception { return deposit(u, a, amount, key, 201); }
    private JsonNode deposit(User u, Account a, String amount, String key, int expected) throws Exception {
        return deposit(u, a, amount, key, "OTHER", "integration deposit", expected);
    }
    private JsonNode deposit(User u, Account a, String amount, String key, String category, String description) throws Exception {
        return deposit(u, a, amount, key, category, description, 201);
    }
    private JsonNode deposit(User u, Account a, String amount, String key, String category, String description, int expected) throws Exception {
        String body = cashBody(a, amount, category, description);
        MvcResult result = mvc.perform(post("/api/v1/transactions/deposits").header("Authorization", bearer(u))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expected)).andReturn();
        return result.getResponse().getContentAsString().isBlank() ? json.createObjectNode() : json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode withdraw(User u, Account a, String amount, String key, String category, String description) throws Exception {
        return withdraw(u, a, amount, key, category, description, 201);
    }
    private JsonNode withdraw(User u, Account a, String amount, String key, String category, String description, int expected) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/transactions/withdrawals").header("Authorization", bearer(u))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                        .content(cashBody(a, amount, category, description)))
                .andExpect(status().is(expected)).andReturn();
        return result.getResponse().getContentAsString().isBlank() ? json.createObjectNode() : json.readTree(result.getResponse().getContentAsString());
    }

    private String cashBody(Account account, String amount, String category, String description) {
        return "{\"accountId\":\"" + account.id() + "\",\"amount\":" + amount + ",\"category\":\"" + category +
                "\",\"description\":\"" + description + "\"}";
    }

    private JsonNode transfer(User u, Account from, Account to, String amount, String key, int expected) throws Exception {
        String body = "{\"sourceAccountId\":\"" + from.id() + "\",\"targetAccountId\":\"" + to.id() + "\",\"amount\":" + amount + ",\"category\":\"OTHER\",\"description\":\"integration transfer\"}";
        MvcResult result = mvc.perform(post("/api/v1/transactions/transfers").header("Authorization", bearer(u))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expected)).andReturn();
        return result.getResponse().getContentAsString().isBlank() ? json.createObjectNode() : json.readTree(result.getResponse().getContentAsString());
    }

    private BigDecimal balance(User u, Account a) throws Exception {
        JsonNode data = getJson(u, "/api/v1/accounts/" + a.id());
        return data.path("balance").decimalValue();
    }
    private JsonNode getJson(User u, String path) throws Exception {
        MvcResult result = mvc.perform(get(path).header("Authorization", bearer(u))).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    private String bearer(User u) { return "Bearer " + u.token(); }
    private static String email() { return "it-" + UUID.randomUUID() + "@example.test"; }
    private static String randomJwtSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
