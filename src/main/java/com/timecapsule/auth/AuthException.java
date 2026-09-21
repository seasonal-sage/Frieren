package com.timecapsule.auth;

/** Friendly, keeper-voiced failure for register/login/session problems. */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
