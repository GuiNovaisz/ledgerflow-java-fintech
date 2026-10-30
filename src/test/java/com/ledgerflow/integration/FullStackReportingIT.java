package com.ledgerflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class FullStackReportingIT {
    private static final boolean EXTERNAL = System.getProperty("test.database.url") != null;
    private static final PostgreSQLContainer<?> POSTGRES = EXTERNAL ? null : new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String SECRET = secret();
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        if (EXTERNAL) {
            properties.add("spring.datasource.url", () -> System.getProperty("test.database.url"));
            properties.add("spring.datasource.username", () -> System.getProperty("test.database.username", "postgres"));
            properties.add("spring.datasource.password", () -> System.getProperty("test.database.password", ""));
        } else {
            if (!POSTGRES.isRunning()) POSTGRES.start();
            properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            properties.add("spring.datasource.username", POSTGRES::getUsername);
            properties.add("spring.datasource.password", POSTGRES::getPassword);
        }
        properties.add("ledgerflow.jwt.secret", () -> SECRET);
    }
    @AfterAll static void stop() { if (POSTGRES != null && POSTGRES.isRunning()) POSTGRES.stop(); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void dashboardConservesBalancesWithoutDoubleCountingOwnTransfers() throws Exception {
        String owner = user(), other = user();
        String first = account(owner, "Main"), second = account(owner, "Savings"), foreign = account(other, "Private foreign account");
        JsonNode deposit = cash(owner, "deposits", first, "100.00", "deposit", "SALARY", "salary");
        cash(other, "deposits", foreign, "50.00", "foreign-deposit", "SALARY", "foreign");
        transfer(owner, first, second, "20.00", "internal-transfer");
        cash(owner, "withdrawals", first, "10.00", "withdrawal", "FOOD", "food");
        transfer(other, foreign, first, "7.00", "incoming-transfer");

        JsonNode dashboard = request(owner, get("/api/v1/dashboard").param("from", Instant.now().minus(1, ChronoUnit.DAYS).toString()), 200);
        assertThat(dashboard.path("totalBalance").asText()).isEqualTo("97.00");
        assertThat(dashboard.path("income").asText()).isEqualTo("107.00");
        assertThat(dashboard.path("expenses").asText()).isEqualTo("10.00");
        assertThat(dashboard.path("transactionCount").asInt()).isEqualTo(4);
        assertThat(dashboard.path("expensesByCategory").path("FOOD").asText()).isEqualTo("10.00");
        JsonNode series = dashboard.path("balanceSeries");
        assertThat(series.get(0).path("balance").asText()).isEqualTo("0.00");
        assertThat(series.get(series.size() - 1).path("balance").asText()).isEqualTo("97.00");
        assertThat(dashboard.toString()).doesNotContain("Private foreign account", foreign);
        JsonNode otherDashboard = request(other, get("/api/v1/dashboard"), 200);
        assertThat(otherDashboard.path("totalBalance").asText()).isEqualTo("43.00");
        assertThat(otherDashboard.path("expenses").asText()).isEqualTo("7.00");

        Instant timestamp = Instant.parse(deposit.path("createdAt").asText());
        JsonNode historical = request(owner, get("/api/v1/dashboard").param("from", timestamp.minusNanos(1).toString())
                .param("to", timestamp.plusNanos(1).toString()), 200);
        assertThat(historical.path("income").asText()).isEqualTo("100.00");
        assertThat(historical.path("transactionCount").asInt()).isEqualTo(1);
        assertThat(historical.path("balanceSeries").get(0).path("balance").asText()).isEqualTo("100.00");
        JsonNode empty = request(owner, get("/api/v1/dashboard").param("from", timestamp.plusNanos(1).toString())
                .param("to", timestamp.plusNanos(2).toString()), 200);
        assertThat(empty.path("income").asText()).isEqualTo("0.00");
        assertThat(empty.path("transactionCount").asInt()).isZero();
        assertThat(empty.path("balanceSeries").get(0).path("balance").asText()).isEqualTo("100.00");
    }

    @Test
    void filtersAreLiteralPaginatedScopedAndCsvNeutralizesText() throws Exception {
        String owner = user(), other = user();
        String account = account(owner, "=Planning"), foreign = account(other, "Foreign");
        cash(owner, "deposits", account, "50.00", "seed", "SALARY", "salary");
        JsonNode operation = cash(owner, "withdrawals", account, "5.25", "expense", "FOOD", "=10%_food");
        cash(owner, "withdrawals", account, "3.00", "transport", "TRANSPORT", "travel");
        JsonNode filtered = request(owner, get("/api/v1/entries").param("q", "%_").param("accountId", account)
                .param("category", "FOOD").param("type", "WITHDRAWAL").param("direction", "DEBIT"), 200);
        assertThat(filtered.path("totalElements").asInt()).isEqualTo(1);
        assertThat(filtered.path("content").get(0).path("amount").asText()).isEqualTo("5.25");
        assertThat(filtered.path("content").get(0).path("operationId").asText()).isEqualTo(operation.path("id").asText());
        assertThat(request(owner, get("/api/v1/entries").param("q", "' OR 1=1 --"), 200).path("totalElements").asInt()).isZero();
        JsonNode page = request(owner, get("/api/v1/entries").param("size", "2").param("page", "1"), 200);
        assertThat(page.path("content").size()).isEqualTo(1);
        assertThat(page.path("totalPages").asInt()).isEqualTo(2);
        assertThat(request(other, get("/api/v1/entries"), 200).path("totalElements").asInt()).isZero();
        request(owner, get("/api/v1/entries").param("accountId", foreign), 404);
        request(other, get("/api/v1/entries/export.csv").param("accountId", account), 404);
        String csv = mvc.perform(get("/api/v1/entries/export.csv").param("q", "%_").header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("\"'=Planning\"", "\"'=10%_food\"", ",5.25,BRL,").doesNotContain("travel", "salary");
        request(owner, get("/api/v1/entries").param("size", "101"), 400);
        request(owner, get("/api/v1/entries").param("q", "x".repeat(101)), 400);
        request(owner, get("/api/v1/dashboard").param("from", "2020-01-01T00:00:00Z").param("to", "2022-01-01T00:00:00Z"), 400);
    }

    @Test
    void operationLookupUsesInitiatorAndDoesNotTreatMissingAsDefinitiveFailure() throws Exception {
        String owner = user(), other = user();
        String account = account(owner, "Main");
        JsonNode operation = cash(owner, "deposits", account, "999999999999999.99", "lookup-key", "SALARY", "exact cents");
        JsonNode found = request(owner, get("/api/v1/operations/by-key/lookup-key"), 200);
        assertThat(found.path("id").asText()).isEqualTo(operation.path("id").asText());
        assertThat(found.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(found.path("amount").asText()).isEqualTo("999999999999999.99");
        assertThat(request(owner, get("/api/v1/dashboard"), 200).path("totalBalance").asText()).isEqualTo("999999999999999.99");
        request(other, get("/api/v1/operations/by-key/lookup-key"), 404);
        JsonNode missing = request(owner, get("/api/v1/operations/by-key/not-yet-committed"), 404);
        assertThat(missing.path("detail").asText()).contains("in-flight request may still complete");
        mvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void goalsAreOwnedVersionedValidatedAndDoNotChangeMoney() throws Exception {
        String owner = user(), other = user();
        String account = account(owner, "Main");
        cash(owner, "deposits", account, "50.00", "goal-seed", "SALARY", "seed");
        Map<String, Object> create = Map.of("name", " Emergency fund ", "targetAmount", "100.00", "currentAmount", "0.00", "dueDate", "2030-12-31");
        JsonNode goal = request(owner, post("/api/v1/goals").content(json.writeValueAsBytes(create)), 201);
        String id = goal.path("id").asText();
        assertThat(goal.path("name").asText()).isEqualTo("Emergency fund");
        assertThat(goal.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(request(other, get("/api/v1/goals"), 200).size()).isZero();
        Map<String, Object> edit = Map.of("name", "Emergency fund", "targetAmount", "100.00", "currentAmount", "120.00", "version", 0);
        request(other, put("/api/v1/goals/" + id).content(json.writeValueAsBytes(edit)), 404);
        request(other, delete("/api/v1/goals/" + id).param("version", "0"), 404);
        JsonNode updated = request(owner, put("/api/v1/goals/" + id).content(json.writeValueAsBytes(edit)), 200);
        assertThat(updated.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(updated.path("version").asInt()).isEqualTo(1);
        request(owner, put("/api/v1/goals/" + id).content(json.writeValueAsBytes(edit)), 409);
        request(owner, delete("/api/v1/goals/" + id).param("version", "0"), 409);
        request(owner, post("/api/v1/goals").content("{\"name\":\"Invalid\",\"targetAmount\":\"1.00\",\"currentAmount\":\"-0.01\"}"), 400);
        request(owner, post("/api/v1/goals").content("{\"name\":\"Invalid\",\"targetAmount\":\"1.001\",\"currentAmount\":\"0.00\"}"), 400);
        request(owner, post("/api/v1/goals").content("{\"name\":\"Invalid\",\"targetAmount\":\"1.00\",\"currentAmount\":\"0.00\",\"ownerId\":\"" + UUID.randomUUID() + "\"}"), 400);
        assertThat(request(owner, get("/api/v1/dashboard"), 200).path("totalBalance").asText()).isEqualTo("50.00");
        request(owner, delete("/api/v1/goals/" + id).param("version", "1"), 204);
        assertThat(request(owner, get("/api/v1/goals"), 200).size()).isZero();
    }

    private String user() throws Exception {
        String email = "insight-" + UUID.randomUUID() + "@example.test";
        String password = "Synthetic-test-password-42";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", email, "password", password, "displayName", "Synthetic user"))))
                .andExpect(status().isCreated());
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("accessToken").asText();
    }
    private String account(String token, String name) throws Exception {
        return request(token, post("/api/v1/accounts").content(json.writeValueAsBytes(Map.of("name", name))), 201).path("id").asText();
    }
    private JsonNode cash(String token, String action, String account, String amount, String key, String category, String description) throws Exception {
        return request(token, post("/api/v1/transactions/" + action).header("Idempotency-Key", key)
                .content(json.writeValueAsBytes(Map.of("accountId", account, "amount", amount, "category", category, "description", description))), 201);
    }
    private JsonNode transfer(String token, String source, String target, String amount, String key) throws Exception {
        return request(token, post("/api/v1/transactions/transfers").header("Idempotency-Key", key)
                .content(json.writeValueAsBytes(Map.of("sourceAccountId", source, "targetAccountId", target, "amount", amount, "category", "OTHER", "description", "transfer"))), 201);
    }
    private JsonNode request(String token, MockHttpServletRequestBuilder request, int expected) throws Exception {
        MvcResult result = mvc.perform(request.header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON))
                .andDo(response -> { if (response.getResponse().getStatus() >= 500 && response.getResolvedException() != null) throw response.getResolvedException(); })
                .andExpect(status().is(expected)).andReturn();
        byte[] content = result.getResponse().getContentAsByteArray();
        return content.length == 0 ? json.nullNode() : json.readTree(content);
    }
    private static String bearer(String token) { return "Bearer " + token; }
    private static String secret() { byte[] bytes = new byte[48]; new SecureRandom().nextBytes(bytes); return Base64.getEncoder().encodeToString(bytes); }
}
