package com.ledgerflow.auth;

import static com.ledgerflow.auth.AuthDtos.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/** Same-origin browser authentication. No access token is returned to JavaScript. */
@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    private final AuthService auth;
    private final boolean secureCookie;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public SessionController(AuthService auth, @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        return establish(auth.authenticate(body), request, response);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest body,
                                 HttpServletRequest request, HttpServletResponse response) {
        return establish(auth.register(body), request, response);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt principal) {
        return auth.profile(UUID.fromString(principal.getSubject()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("LEDGERFLOW_SESSION", "")
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/").maxAge(0).build().toString());
    }

    private UserResponse establish(UserResponse user, HttpServletRequest request, HttpServletResponse response) {
        // Replace the anonymous/previous session entirely: rotate both ID and CSRF secret.
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        request.getSession(true);
        var context = SecurityContextHolder.createEmptyContext();
        // Jwt is only a principal adapter for the existing resource controllers. This is
        // neither a signed access token nor a bearer credential and never leaves the server.
        Jwt principal = Jwt.withTokenValue("server-session-principal")
                .header("typ", "SERVER_SESSION").subject(user.id().toString()).build();
        // Bearer JwtAuthenticationToken is marked @Transient by Spring Security and
        // deliberately cannot be persisted in HttpSession. Use a session authentication
        // with the same principal shape, retaining compatibility with resource controllers.
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return user;
    }
}
