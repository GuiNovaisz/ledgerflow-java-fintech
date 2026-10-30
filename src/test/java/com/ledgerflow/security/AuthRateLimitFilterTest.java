package com.ledgerflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    @Test
    void limitsAuthenticationBeforeHandlersAndRecoversAfterWindow() throws Exception {
        AtomicLong clock = new AtomicLong(10_000);
        AuthRateLimitFilter limiter = new AuthRateLimitFilter(new ObjectMapper(), clock::get);
        AtomicInteger handled = new AtomicInteger();
        for (int i = 0; i < 120; i++) {
            assertThat(send(limiter, "/api/v1/session/login", "127.0.0.1", handled).getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse rejected = send(limiter, "/api/v1/auth/register", "127.0.0.1", handled);
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentAsString()).contains("RATE_LIMITED");
        assertThat(handled.get()).isEqualTo(120);
        assertThat(send(limiter, "/api/v1/accounts", "127.0.0.1", handled).getStatus()).isEqualTo(200);
        assertThat(send(limiter, "/api/v1/session/login", "127.0.0.2", handled).getStatus()).isEqualTo(200);
        clock.addAndGet(60_001);
        assertThat(send(limiter, "/api/v1/session/login", "127.0.0.1", handled).getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse send(AuthRateLimitFilter limiter, String path, String ip, AtomicInteger handled) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        request.addHeader("X-Forwarded-For", "different-untrusted-client");
        MockHttpServletResponse response = new MockHttpServletResponse();
        limiter.doFilter(request, response, (req, res) -> handled.incrementAndGet());
        return response;
    }
}
