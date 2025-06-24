package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for tenant information API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoRequestDTO {
    
    /** The assistant ID to look up */
    private Integer assistantId;
} 