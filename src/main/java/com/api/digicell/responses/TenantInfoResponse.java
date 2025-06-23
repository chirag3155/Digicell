package com.api.digicell.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for tenant information including tenant ID and quickation items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoResponse {
    
    /** The tenant ID */
    private String tenantId;
    
    /** List of quickation items */
    private List<String> quickationItems;
} 