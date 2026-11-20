package com.parkhfx.auth;

/** Registration with an email that already has an account. Returned as 409. */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("An account with that email already exists.");
    }
}
