package com.ledgerflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    AuthRateLimitFilter authRateLimitFilter(ObjectMapper mapper) { return new AuthRateLimitFilter(mapper); }

    @Bean
    FilterRegistrationBean<AuthRateLimitFilter> disableAutomaticRateFilterRegistration(AuthRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** An explicit Authorization header always selects stateless authentication.
     * A fake/malformed bearer header therefore cannot bypass CSRF using a session cookie. */
    @Bean @Order(1)
    SecurityFilterChain bearerApi(HttpSecurity http, ObjectMapper mapper, AuthRateLimitFilter limiter) throws Exception {
        http.securityMatcher(request -> request.getHeader("Authorization") != null
                        || request.getRequestURI().startsWith(request.getContextPath() + "/api/v1/auth/"))
                .csrf(csrf -> csrf.disable())
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .addFilterBefore(limiter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/session/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized(mapper)))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized(mapper))
                        .accessDeniedHandler(forbidden(mapper)));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain browserSession(HttpSecurity http, ObjectMapper mapper, AuthRateLimitFilter limiter) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .addFilterBefore(limiter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/session/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/session/login", "/api/v1/session/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized(mapper))
                        .accessDeniedHandler(forbidden(mapper)));
        return http.build();
    }

    private AuthenticationEntryPoint unauthorized(ObjectMapper mapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", "Unauthorized",
                    "status", 401, "code", "AUTHENTICATION_REQUIRED", "detail", "Authentication is required"));
        };
    }

    private AccessDeniedHandler forbidden(ObjectMapper mapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", "Forbidden",
                    "status", 403, "code", "ACCESS_DENIED", "detail", "Access is denied"));
        };
    }
}
