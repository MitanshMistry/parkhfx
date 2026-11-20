package com.parkhfx.auth;

import java.util.UUID;

import com.parkhfx.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the auth API. Entities and password hashes never leave the server. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt only uses the first 72 bytes, so longer passwords are rejected up front.
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName) {
    }

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {
    }

    public record UserDto(UUID id, String email, String displayName) {
        public static UserDto from(User user) {
            return new UserDto(user.id(), user.email(), user.displayName());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
