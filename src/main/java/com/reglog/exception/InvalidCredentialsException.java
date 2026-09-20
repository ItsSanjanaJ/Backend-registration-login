package com.reglog.exception;

/**
 * Thrown when credentials are invalid during login.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}