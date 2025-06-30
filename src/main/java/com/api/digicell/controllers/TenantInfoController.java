package com.api.digicell.controllers;

import com.api.digicell.dto.TenantInfoRequestDTO;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.responses.TenantInfoResponse;
import com.api.digicell.services.TenantInfoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tenant Information", description = "APIs for retrieving tenant information and quickation items")
@SecurityRequirement(name = "bearerAuth")
public class TenantInfoController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantInfoController.class);
    private final TenantInfoService tenantInfoService;
    
    /**
     * Get tenant information by assistant ID
     */
    @Operation(
        summary = "Get tenant information by assistant ID",
        description = "Retrieves tenant ID and quickation items based on the provided assistant ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping("/info")
    public ResponseEntity<ApiResponse<TenantInfoResponse>> getTenantInfo(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @RequestBody TenantInfoRequestDTO request) {
        
        logger.info("Received request to get tenant info for assistant ID: {}", request.getAssistantId());
        
        try {
            TenantInfoResponse tenantInfo = tenantInfoService.getTenantInfoByAssistantId(request.getAssistantId());
            logger.debug("Successfully retrieved tenant info for assistant ID: {}", request.getAssistantId());
            
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Tenant information retrieved successfully", tenantInfo));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid assistant ID provided: {}", request.getAssistantId());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
                    
        } catch (Exception e) {
            logger.error("Error retrieving tenant info for assistant ID {}: {}", request.getAssistantId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error retrieving tenant information", null));
        }
    }
} 