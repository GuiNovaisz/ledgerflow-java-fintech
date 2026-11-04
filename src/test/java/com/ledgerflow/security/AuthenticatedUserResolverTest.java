package com.ledgerflow.security;

import com.ledgerflow.account.api.AccountController;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserResolverTest {
    private final AuthenticatedUserResolver resolver = new AuthenticatedUserResolver();
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void resolvesBearerOrSessionPrincipalWithoutControllerParsing() throws Exception {
        UUID owner = UUID.randomUUID();
        MethodParameter parameter = new MethodParameter(AccountController.class.getMethod("list", UUID.class), 0);
        assertThat(resolver.supportsParameter(parameter)).isTrue();
        Jwt jwt = Jwt.withTokenValue("test-principal").header("alg", "none").subject(owner.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(jwt, null, List.of()));
        assertThat(resolver.resolveArgument(parameter, null, null, null)).isEqualTo(owner);
    }

    @Test void rejectsMissingOrWrongPrincipal() throws Exception {
        MethodParameter parameter = new MethodParameter(AccountController.class.getMethod("list", UUID.class), 0);
        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("someone-else", null, List.of()));
        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
