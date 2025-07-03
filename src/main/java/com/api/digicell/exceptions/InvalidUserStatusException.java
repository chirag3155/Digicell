package com.api.digicell.exceptions;

/**
 * Exception thrown when an invalid user status is provided.
 */
public class InvalidUserStatusException extends IllegalArgumentException {
    public InvalidUserStatusException(String status) {
        super(String.format("Invalid user status: '%s'. Valid statuses are: ONLINE OR OFFLINE", status));
    }
} 