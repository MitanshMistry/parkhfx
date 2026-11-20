package com.parkhfx.auth;

/** Login failed, or a valid token points at an account that no longer exists. Returned as 401. */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
