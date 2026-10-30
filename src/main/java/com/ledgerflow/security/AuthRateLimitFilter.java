package com.ledgerflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** Small single-process abuse guard, not a substitute for an edge/distributed limiter. */
public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PATHS = Set.of("/api/v1/auth/login", "/api/v1/auth/register",
            "/api/v1/session/login", "/api/v1/session/register");
    private static final int LIMIT = 120;
    private static final int MAX_CLIENTS = 2048;
    private static final long WINDOW_MILLIS = 60_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final LongSupplier clock;

    public AuthRateLimitFilter(ObjectMapper json) { this(json, System::currentTimeMillis); }
    AuthRateLimitFilter(ObjectMapper json, LongSupplier clock) { this.json = json; this.clock = clock; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !PATHS.contains(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.getAsLong();
        String client = request.getRemoteAddr(); // Never trust arbitrary X-Forwarded-For headers.
        if (!windows.containsKey(client) && windows.size() >= MAX_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= WINDOW_MILLIS);
            if (windows.size() >= MAX_CLIENTS) { reject(response); return; }
        }
        AtomicBoolean allowed = new AtomicBoolean();
        windows.compute(client, (key, existing) -> {
            Window current = existing == null || now - existing.start() >= WINDOW_MILLIS
                    ? new Window(now, 0) : existing;
            if (current.count() < LIMIT) {
                allowed.set(true);
                return new Window(current.start(), current.count() + 1);
            }
            return current;
        });
        if (!allowed.get()) { reject(response); return; }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", "Too Many Requests",
                "status", 429, "code", "RATE_LIMITED", "detail", "Too many authentication attempts. Try again later."));
    }

    private record Window(long start, int count) { }
}
