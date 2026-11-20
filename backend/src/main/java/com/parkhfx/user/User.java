package com.parkhfx.user;

import java.util.UUID;

/** A user account. One account type: anyone can both host and drive. */
public record User(UUID id, String email, String passwordHash, String displayName) {
}
