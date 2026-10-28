package com.ledgerflow.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ledgerflow.auth.AuthDtos.LoginRequest;
import com.ledgerflow.auth.AuthDtos.RegisterRequest;
import com.ledgerflow.auth.AuthDtos.TokenResponse;
import com.ledgerflow.auth.AuthDtos.UserResponse;
import com.ledgerflow.security.JwtTokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenService tokens;
    AuthService service;

    @BeforeEach
    void setUp() { service = new AuthService(users, passwordEncoder, tokens); }

    @Test
    void registerNormalizesEmailAndPersistsOnlyEncodedPassword() {
        when(passwordEncoder.encode("long-enough-password")).thenReturn("$2a$12$encoded-value");
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = service.register(new RegisterRequest("  Alice@Example.COM ", "long-enough-password", " Alice "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertEquals("alice@example.com", captor.getValue().getEmail());
        assertEquals("$2a$12$encoded-value", captor.getValue().getPasswordHash());
        assertNotEquals("long-enough-password", captor.getValue().getPasswordHash());
        assertEquals("Alice", response.displayName());
        assertEquals(captor.getValue().getId(), response.id());
    }

    @Test
    void duplicateRegistrationIsConflictAndDoesNotHashOrSave() {
        when(users.existsByEmail("alice@example.com")).thenReturn(true);
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.register(new RegisterRequest("Alice@Example.com", "long-enough-password", "Alice")));
        assertTrue(error.getMessage().contains("Email is already registered"));
        verify(passwordEncoder, never()).encode(any());
        verify(users, never()).save(any());
    }

    @Test
    void passwordLengthUsesUtf8Bytes() {
        when(passwordEncoder.encode("😀😀😀")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        service.register(new RegisterRequest("a@example.com", "😀😀😀", "A")); // 12 UTF-8 bytes
        verify(passwordEncoder).encode("😀😀😀");

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.register(new RegisterRequest("b@example.com", "😀".repeat(19), "B"))); // 76 bytes
        assertTrue(error.getMessage().contains("12 to 72 UTF-8 bytes"));
        verify(users, times(1)).save(any(User.class));
    }

    @Test
    void missingAndWrongCredentialsHaveSamePublicErrorAndUsePasswordCheck() {
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        RuntimeException missing = assertThrows(RuntimeException.class,
                () -> service.login(new LoginRequest("missing@example.com", "wrong-password")));
        verify(passwordEncoder).matches("wrong-password", "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6h7xF9Q3t2JZ6k9P8mR2Y7qW4N5uS");

        User user = new User("alice@example.com", "real-hash", "Alice");
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "real-hash")).thenReturn(false);
        RuntimeException wrong = assertThrows(RuntimeException.class,
                () -> service.login(new LoginRequest("alice@example.com", "wrong-password")));

        assertEquals(missing.getMessage(), wrong.getMessage());
        assertTrue(missing.getMessage().contains("Invalid email or password"));
        verify(tokens, never()).issue(any());
    }

    @Test
    void loginIssuesTokenForVerifiedUser() {
        User user = new User("alice@example.com", "real-hash", "Alice");
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "real-hash")).thenReturn(true);
        TokenResponse expected = new TokenResponse("signed-token", "Bearer", 900);
        when(tokens.issue(user.getId())).thenReturn(expected);

        assertEquals(expected, service.login(new LoginRequest("ALICE@example.com", "correct-password")));
        verify(tokens).issue(user.getId());
    }

    @Test
    void realBcryptDoesNotAcceptSuffixBeyondSeventyTwoBytes() {
        var bcrypt = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        String password = "a".repeat(72);
        User user = new User("boundary@example.com", bcrypt.encode(password), "Boundary");
        when(users.findByEmail("boundary@example.com")).thenReturn(Optional.of(user));
        AuthService realPasswordService = new AuthService(users, bcrypt, tokens);
        when(tokens.issue(user.getId())).thenReturn(new TokenResponse("test-token", "Bearer", 900));
        assertNotNull(realPasswordService.login(new LoginRequest("boundary@example.com", password)));
        var error = assertThrows(com.ledgerflow.shared.BusinessException.class,
                () -> realPasswordService.login(new LoginRequest("boundary@example.com", password + "suffix")));
        assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, error.getStatus());
        assertEquals("Invalid email or password", error.getMessage());
        verify(tokens, times(1)).issue(any());
    }
}
