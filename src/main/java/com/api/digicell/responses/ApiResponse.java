package com.api.digicell.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic wrapper for REST responses.
 *
 * @param <T> the type of data being returned to the client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /** HTTP status code */
    private int statusCode;

    /** Human readable message conveying the result */
    private String message;

    /** Payload */
    private T data;
} 