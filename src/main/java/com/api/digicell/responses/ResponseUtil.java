package com.api.digicell.responses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Utility helpers for creating consistent {@link ApiResponse} instances.
 */
public final class ResponseUtil {

    private ResponseUtil() {}

    /**
     * Creates a ResponseEntity for list endpoints which returns 404 + "No <entityName> found" when the list is empty,
     * otherwise 200 + "<entityName> fetched successfully".
     *
     * @param list       the data list to wrap
     * @param entityName human-readable entity label (plural)
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> listResponse(List<T> list, String entityName) {
        if (list == null || list.isEmpty()) {
            ApiResponse<List<T>> body = new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "No " + entityName + " found", List.of());
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
        ApiResponse<List<T>> body = new ApiResponse<>(HttpStatus.OK.value(), entityName + " fetched successfully", list);
        return ResponseEntity.ok(body);
    }
} 