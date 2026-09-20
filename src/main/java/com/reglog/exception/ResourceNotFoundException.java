package com.reglog.exception;

/**
 * Thrown when a requested resource (e.g. a user) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}