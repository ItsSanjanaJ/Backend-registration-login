package com.reglog.exception;

/**
 * Thrown when a requested business operation is not allowed,
 * e.g. signup with an existing username/email.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}