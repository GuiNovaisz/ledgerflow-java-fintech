package com.ledgerflow.auth;

import com.ledgerflow.auth.AuthDtos.LoginRequest;
import com.ledgerflow.auth.AuthDtos.RegisterRequest;
import com.ledgerflow.auth.AuthDtos.TokenResponse;
import com.ledgerflow.auth.AuthDtos.UserResponse;
import com.ledgerflow.security.JwtTokenService;
import com.ledgerflow.shared.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String DUMMY_HASH = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6h7xF9Q3t2JZ6k9P8mR2Y7qW4N5uS";
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        validatePasswordBytes(request.password());
        if (users.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "email_already_registered", "Email is already registered");
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        return tokens.issue(authenticate(request).id());
    }

    /** Shared credential verification for the stateless API and browser session. */
    @Transactional(readOnly = true)
    public UserResponse authenticate(LoginRequest request) {
        String email = normalize(request.email());
        int passwordBytes = request.password().getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            passwordEncoder.matches("invalid-password", DUMMY_HASH);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password");
        }
        User user = users.findByEmail(email).orElse(null);
        String hash = user == null ? DUMMY_HASH : user.getPasswordHash();
        boolean valid = passwordEncoder.matches(request.password(), hash);
        if (user == null || !valid) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password");
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse profile(java.util.UUID id) {
        return users.findById(id).map(UserResponse::from).orElseThrow(() ->
                new BusinessException(HttpStatus.UNAUTHORIZED, "invalid_session", "Authentication is required"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePasswordBytes(String password) {
        int length = password.getBytes(StandardCharsets.UTF_8).length;
        if (length < 12 || length > 72) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "invalid_password_length", "Password must be 12 to 72 UTF-8 bytes");
        }
    }
}
