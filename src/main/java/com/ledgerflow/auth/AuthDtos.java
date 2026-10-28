package com.ledgerflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password,
            @NotBlank @Size(max = 100) String displayName) {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {}

    public record UserResponse(UUID id, String email, String displayName) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
        }
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
