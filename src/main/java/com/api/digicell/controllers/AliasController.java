package com.api.digicell.controllers;

import com.api.digicell.entities.Alias;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.services.AliasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import com.api.digicell.responses.ResponseUtil;

import java.util.List;

/**
 * REST endpoints for Alias CRUD operations.
 * <p>
 * Example usage:
 * POST   /api/v1/aliases  {"key": "wlc", "value": "Welcome!"}
 * GET    /api/v1/aliases/wlc
 * PUT    /api/v1/aliases/wlc  {"key": "wlc", "value": "Welcome to Digicell"}
 * DELETE /api/v1/aliases/wlc
 * </p>
 */
@RestController
@RequestMapping("/api/v1/aliases")
@RequiredArgsConstructor
@Validated
public class AliasController {

    private final AliasService aliasService;

    /**
     * Create a new alias.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Alias>> createAlias(@Valid @RequestBody Alias alias) {
        Alias created = aliasService.createAlias(alias);
        ApiResponse<Alias> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Alias created successfully", created);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieve all aliases.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Alias>>> listAliases() {
        List<Alias> aliases = aliasService.getAllAliases();
        return ResponseUtil.listResponse(aliases, "aliases");
    }

    /**
     * Fetch alias by key.
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Alias>> getAlias(@PathVariable @NotBlank(message = "key must not be blank") String key) {
        Alias alias = aliasService.getAliasByKey(key);
        ApiResponse<Alias> response = new ApiResponse<>(HttpStatus.OK.value(), "Alias fetched successfully", alias);
        return ResponseEntity.ok(response);
    }

    /**
     * Update alias by key.
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<Alias>> updateAlias(@PathVariable @NotBlank(message = "key must not be blank") String key, @Valid @RequestBody Alias updatedAlias) {
        Alias alias = aliasService.updateAlias(key, updatedAlias);
        ApiResponse<Alias> response = new ApiResponse<>(HttpStatus.OK.value(), "Alias updated successfully", alias);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete alias by key.
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteAlias(@PathVariable @NotBlank(message = "key must not be blank") String key) {
        aliasService.deleteAlias(key);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.NO_CONTENT.value(), "Alias deleted successfully", null);
        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
} 