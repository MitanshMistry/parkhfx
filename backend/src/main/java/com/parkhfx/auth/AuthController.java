package com.parkhfx.auth;

import java.util.UUID;

import com.parkhfx.auth.AuthDtos.AuthResponse;
import com.parkhfx.auth.AuthDtos.LoginRequest;
import com.parkhfx.auth.AuthDtos.RegisterRequest;
import com.parkhfx.auth.AuthDtos.UserDto;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    /** One-click login for reviewers. 404 when demo mode is switched off. */
    @PostMapping("/auth/demo")
    public ResponseEntity<AuthResponse> demo() {
        return auth.demoLogin()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** The signed-in user, taken from the token's subject. */
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return auth.currentUser(UUID.fromString(jwt.getSubject()));
    }
}
