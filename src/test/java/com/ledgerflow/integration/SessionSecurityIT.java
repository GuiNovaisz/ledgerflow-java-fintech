package com.ledgerflow.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Real HTTP tests: cookies, CSRF, fixation and revocation require a servlet container. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SessionSecurityIT.SessionProbeConfiguration.class)
@Timeout(60)
class SessionSecurityIT {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final String PASSWORD = "synthetic-test-password-2026";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        POSTGRES.start();
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        byte[] secret = new byte[48];
        new SecureRandom().nextBytes(secret);
        String encoded = Base64.getEncoder().encodeToString(secret);
        properties.add("ledgerflow.jwt.secret", () -> encoded);
    }

    @AfterAll static void stop() { POSTGRES.stop(); }
    @Autowired ObjectMapper json;
    @Autowired ServerProperties server;
    @Autowired SessionProbe sessions;
    @LocalServerPort int port;

    @Test
    void registrationRotatesSessionAndCsrfAndUsesHttpOnlyCookie() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> initial = browser.send("GET", "/api/v1/session/csrf", null, null, null);
        assertThat(initial.statusCode()).isEqualTo(200);
        String cookie = initial.headers().firstValue("set-cookie").orElseThrow();
        assertThat(cookie.contains("HttpOnly")).isTrue();
        assertThat(cookie.contains("SameSite=Strict")).isTrue();
        assertThat(cookie.contains("Path=/")).isTrue();
        assertThat(server.getServlet().getSession().getTimeout()).isEqualTo(Duration.ofMinutes(15));
        assertThat(server.getServlet().getSession().getCookie().getHttpOnly()).isTrue();
        String anonymousId = browser.sessionId();
        JsonNode csrf = json.readTree(initial.body());
        HttpResponse<String> registration = browser.send("POST", "/api/v1/session/register", registration(), csrf, null);
        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(browser.sessionId().equals(anonymousId)).isFalse();
        JsonNode profile = json.readTree(registration.body());
        assertThat(profile.has("accessToken")).isFalse();
        assertThat(profile.has("password")).isFalse();
        assertThat(profile.has("passwordHash")).isFalse();
        assertThat(browser.send("GET", "/api/v1/session/me", null, null, null).statusCode()).isEqualTo(200);
        assertThat(browser.send("POST", "/api/v1/accounts", Map.of("name", "Old CSRF"), csrf, null).statusCode()).isEqualTo(403);
        assertThat(browser.send("POST", "/api/v1/accounts", Map.of("name", "My account"), browser.csrf(), null).statusCode()).isEqualTo(201);
    }

    @Test
    void loginRequiresCsrfAndUsesGenericCredentialErrors() throws Exception {
        Browser browser = new Browser();
        Map<String, String> user = registration();
        assertThat(browser.send("POST", "/api/v1/session/register", user, null, null).statusCode()).isEqualTo(403);
        assertThat(browser.send("POST", "/api/v1/session/login", credentials(user), null, null).statusCode()).isEqualTo(403);
        assertThat(browser.send("POST", "/api/v1/session/register", user, browser.csrf(), null).statusCode()).isEqualTo(201);
        assertThat(browser.send("POST", "/api/v1/session/logout", null, browser.csrf(), null).statusCode()).isEqualTo(204);
        JsonNode csrf = browser.csrf();
        HttpResponse<String> missing = browser.send("POST", "/api/v1/session/login", Map.of("email", "missing@example.test", "password", PASSWORD), csrf, null);
        HttpResponse<String> wrong = browser.send("POST", "/api/v1/session/login", Map.of("email", user.get("email"), "password", "wrong-test-password"), csrf, null);
        assertThat(missing.statusCode()).isEqualTo(401);
        assertThat(wrong.statusCode()).isEqualTo(401);
        assertThat(json.readTree(missing.body()).path("code").asText()).isEqualTo(json.readTree(wrong.body()).path("code").asText());
        String anonymousId = browser.sessionId();
        assertThat(browser.send("POST", "/api/v1/session/login", credentials(user), csrf, null).statusCode()).isEqualTo(200);
        assertThat(browser.sessionId().equals(anonymousId)).isFalse();
    }

    @Test
    void cookieMutationsRequireCsrfAndAuthorizationHeaderCannotBypassIt() throws Exception {
        Browser browser = registered();
        Map<String, String> account = Map.of("name", "Must not be created");
        assertThat(browser.send("POST", "/api/v1/accounts", account, null, null).statusCode()).isEqualTo(403);
        assertThat(browser.send("POST", "/api/v1/accounts", account, null, "Bearer invalid-token").statusCode()).isEqualTo(401);
        assertThat(browser.send("POST", "/api/v1/accounts", account, null, "Basic invalid").statusCode()).isEqualTo(401);
        assertThat(json.readTree(browser.send("GET", "/api/v1/accounts", null, null, null).body()).size()).isZero();
        assertThat(browser.send("POST", "/api/v1/accounts", Map.of("name", "Allowed"), browser.csrf(), null).statusCode()).isEqualTo(201);
    }

    @Test
    void logoutRevokesSessionOnServerAndRejectsReplay() throws Exception {
        Browser browser = registered();
        String previousId = browser.sessionId();
        JsonNode previousCsrf = browser.csrf();
        HttpResponse<String> logout = browser.send("POST", "/api/v1/session/logout", null, previousCsrf, null);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().allValues("set-cookie").stream().anyMatch(value -> value.contains("Max-Age=0"))).isTrue();
        assertThat(browser.send("GET", "/api/v1/session/me", null, null, null).statusCode()).isEqualTo(401);
        HttpRequest replay = HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Cookie", "LEDGERFLOW_SESSION=" + previousId).GET().build();
        assertThat(HttpClient.newHttpClient().send(replay, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test
    void idleExpiredSessionCannotReadProtectedData() throws Exception {
        Browser browser = registered();
        String id = browser.sessionId().replace("\"", "");
        HttpSession session = sessions.active.get(id);
        assertThat(session).isNotNull();
        session.setMaxInactiveInterval(1); // Test-only short timeout on the actual servlet session.
        Thread.sleep(2200);
        assertThat(browser.send("GET", "/api/v1/session/me", null, null, null).statusCode()).isEqualTo(401);
        assertThat(browser.send("GET", "/api/v1/accounts", null, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void sessionUsersCannotReadOrChangeOtherAccounts() throws Exception {
        Browser owner = registered();
        Browser other = registered();
        JsonNode account = json.readTree(owner.send("POST", "/api/v1/accounts", Map.of("name", "Private"), owner.csrf(), null).body());
        String path = "/api/v1/accounts/" + account.path("id").asText();
        assertThat(other.send("GET", path, null, null, null).statusCode()).isEqualTo(404);
        assertThat(other.send("PATCH", path + "/status", Map.of("status", "INACTIVE"), other.csrf(), null).statusCode()).isEqualTo(404);
        assertThat(other.send("GET", path + "/entries", null, null, null).statusCode()).isEqualTo(404);
    }

    @Test
    void bearerApiRemainsStatelessAndCannotCreateBrowserSessions() throws Exception {
        Browser api = new Browser();
        Map<String, String> user = registration();
        assertThat(api.send("POST", "/api/v1/auth/register", user, null, null).statusCode()).isEqualTo(201);
        HttpResponse<String> login = api.send("POST", "/api/v1/auth/login", credentials(user), null, null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(api.cookies.getCookieStore().getCookies()).isEmpty();
        String bearer = "Bearer " + json.readTree(login.body()).path("accessToken").asText();
        assertThat(api.send("POST", "/api/v1/accounts", Map.of("name", "Bearer account"), null, bearer).statusCode()).isEqualTo(201);
        assertThat(api.send("GET", "/api/v1/session/me", null, null, bearer).statusCode()).isEqualTo(403);
        assertThat(api.send("GET", "/api/v1/accounts", null, null, null).statusCode()).isEqualTo(401);
        assertThat(api.cookies.getCookieStore().getCookies()).isEmpty();
    }

    @Test
    void browserEndpointsDoNotAllowCrossOriginRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/session/csrf"))
                .header("Origin", "https://attacker.example").GET().build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        HttpRequest preflight = HttpRequest.newBuilder(uri("/api/v1/session/login"))
                .header("Origin", "https://attacker.example").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-CSRF-TOKEN").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        assertThat(HttpClient.newHttpClient().send(preflight, HttpResponse.BodyHandlers.ofString()).headers()
                .firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private Browser registered() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.send("POST", "/api/v1/session/register", registration(), browser.csrf(), null).statusCode()).isEqualTo(201);
        return browser;
    }
    private Map<String, String> registration() {
        return Map.of("displayName", "Session test", "email", UUID.randomUUID() + "@example.test", "password", PASSWORD);
    }
    private Map<String, String> credentials(Map<String, String> user) {
        return Map.of("email", user.get("email"), "password", user.get("password"));
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    @TestConfiguration
    static class SessionProbeConfiguration {
        @Bean SessionProbe sessionProbe() { return new SessionProbe(); }
        @Bean ServletListenerRegistrationBean<SessionProbe> sessionProbeRegistration(SessionProbe probe) {
            return new ServletListenerRegistrationBean<>(probe);
        }
    }
    static class SessionProbe implements HttpSessionListener {
        final Map<String, HttpSession> active = new ConcurrentHashMap<>();
        @Override public void sessionCreated(HttpSessionEvent event) { active.put(event.getSession().getId(), event.getSession()); }
        @Override public void sessionDestroyed(HttpSessionEvent event) { active.remove(event.getSession().getId()); }
    }
    private final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();
        JsonNode csrf() throws Exception { return json.readTree(send("GET", "/api/v1/session/csrf", null, null, null).body()); }
        String sessionId() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("LEDGERFLOW_SESSION")).findFirst().orElseThrow().getValue(); }
        HttpResponse<String> send(String method, String path, Object body, JsonNode csrf, String authorization) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20));
            if (body != null) request.header("Content-Type", "application/json");
            if (csrf != null) request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
            if (authorization != null) request.header("Authorization", authorization);
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
