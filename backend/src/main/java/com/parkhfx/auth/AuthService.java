package com.parkhfx.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.parkhfx.auth.AuthDtos.AuthResponse;
import com.parkhfx.auth.AuthDtos.LoginRequest;
import com.parkhfx.auth.AuthDtos.RegisterRequest;
import com.parkhfx.auth.AuthDtos.UserDto;
import com.parkhfx.user.User;
import com.parkhfx.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    /** Reviewers sign in as this account with one click. Nobody can log in to it with a password. */
    public static final String DEMO_EMAIL = "demo@parkhfx.example";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final boolean demoEnabled;

    /** Compared against when an email is unknown, so "no such user" and "wrong password" take similar time. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                       @Value("${app.demo.enabled:true}") boolean demoEnabled) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.demoEnabled = demoEnabled;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    public AuthResponse register(RegisterRequest request) {
        try {
            User user = users.insert(
                    normalize(request.email()),
                    passwordEncoder.encode(request.password()),
                    request.displayName().trim());
            return respond(user);
        } catch (DuplicateKeyException e) {
            throw new EmailAlreadyUsedException();
        }
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> found = users.findByEmail(normalize(request.email()));
        String hash = found.map(User::passwordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordMatches) {
            // Same message either way, so the API does not reveal which emails are registered.
            throw new AuthenticationFailedException("Invalid email or password.");
        }
        return respond(found.get());
    }

    /** Signs in as the shared demo account (created on first use). Empty when demo login is disabled. */
    public Optional<AuthResponse> demoLogin() {
        if (!demoEnabled) {
            return Optional.empty();
        }
        User user = users.findByEmail(DEMO_EMAIL).orElseGet(this::createDemoUser);
        return Optional.of(respond(user));
    }

    public UserDto currentUser(UUID userId) {
        return users.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new AuthenticationFailedException("Account no longer exists."));
    }

    private User createDemoUser() {
        try {
            // Random password: the demo account can only be entered through the demo endpoint.
            return users.insert(DEMO_EMAIL, passwordEncoder.encode(UUID.randomUUID().toString()), "Demo User");
        } catch (DuplicateKeyException e) {
            // Two first-time demo logins raced; the other one created it.
            return users.findByEmail(DEMO_EMAIL).orElseThrow();
        }
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(tokens.issue(user), UserDto.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
