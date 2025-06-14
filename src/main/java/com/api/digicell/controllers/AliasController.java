package com.api.digicell.controllers;

import com.api.digicell.dto.AliasCreateDTO;
import com.api.digicell.dto.AliasResponseDTO;
import com.api.digicell.dto.AliasUpdateDTO;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.services.AliasService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * PUT    /api/v1/aliases/wlc  {"value": "Welcome to Digicell"}
 * DELETE /api/v1/aliases/wlc
 * </p>
 */
@RestController
@RequestMapping("/api/v1/aliases")
@RequiredArgsConstructor
@Validated
public class AliasController {
    private static final Logger logger = LoggerFactory.getLogger(AliasController.class);
    private final AliasService aliasService;

    /**
     * Create a new alias.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AliasResponseDTO>> createAlias(@Valid @RequestBody AliasCreateDTO createDTO) {
        logger.info("Received request to create new alias");
        logger.debug("Create alias request details - key: {}, value: {}", createDTO.getKey(), createDTO.getValue());
        
        AliasResponseDTO created = aliasService.createAlias(createDTO);
        logger.info("Successfully created alias with key: {}", created.getKey());
        logger.debug("Created alias response - key: {}, value: {}", created.getKey(), created.getValue());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Alias created successfully", created));
    }

    /**
     * Retrieve all aliases.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AliasResponseDTO>>> listAliases() {
        logger.info("Received request to fetch all aliases");
        List<AliasResponseDTO> aliases = aliasService.getAllAliases();
        logger.info("Successfully retrieved {} aliases", aliases.size());
        logger.debug("Retrieved aliases - count: {}, keys: {}", 
            aliases.size(), aliases.stream().map(AliasResponseDTO::getKey).collect(java.util.stream.Collectors.toList()));
        
        return ResponseUtil.listResponse(aliases, "aliases");
    }

    /**
     * Fetch alias by key.
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<AliasResponseDTO>> getAlias(
            @PathVariable @NotBlank(message = "key must not be blank") String key) {
        logger.info("Received request to fetch alias with key: {}", key);
        AliasResponseDTO alias = aliasService.getAliasByKey(key);
        logger.info("Successfully retrieved alias with key: {}", key);
        logger.debug("Retrieved alias details - key: {}, value: {}", alias.getKey(), alias.getValue());
        
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Alias fetched successfully", alias));
    }

    /**
     * Update alias by key.
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<AliasResponseDTO>> updateAlias(
            @PathVariable @NotBlank(message = "key must not be blank") String key,
            @Valid @RequestBody AliasUpdateDTO updateDTO) {
        logger.info("Received request to update alias with key: {}", key);
        logger.debug("Update alias request details - key: {}, new value: {}", key, updateDTO.getValue());
        
        AliasResponseDTO alias = aliasService.updateAlias(key, updateDTO);
        logger.info("Successfully updated alias with key: {}", key);
        logger.debug("Updated alias details - key: {}, value: {}", alias.getKey(), alias.getValue());
        
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Alias updated successfully", alias));
    }

    /**
     * Delete alias by key.
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteAlias(
            @PathVariable @NotBlank(message = "key must not be blank") String key) {
        logger.info("Received request to delete alias with key: {}", key);
        aliasService.deleteAlias(key);
        logger.info("Successfully deleted alias with key: {}", key);
        logger.debug("Deleted alias key: {}", key);
        
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Alias deleted successfully", null));
    }
} 