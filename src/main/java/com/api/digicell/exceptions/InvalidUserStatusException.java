package com.api.digicell.exceptions;

/**
 * Exception thrown when an invalid agent status is provided.
 */
public class InvalidUserStatusException extends IllegalArgumentException {
    public InvalidUserStatusException(String status) {
        super(String.format("Invalid agent status: '%s'. Valid statuses are: AVAILABLE, BREAK, LOGOUT", status));
    }
} 