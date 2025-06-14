package com.api.digicell.exceptions;

/**
 * Exception thrown when an invalid agent status is provided.
 */
public class InvalidAgentStatusException extends IllegalArgumentException {
    public InvalidAgentStatusException(String status) {
        super(String.format("Invalid agent status: '%s'. Valid statuses are: ONLINE and  OFFLINE", status));
    }
} 