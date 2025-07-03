package com.api.digicell.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("assistant_id")
    private Integer assistantId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @JsonProperty("options")
    private List<String> options;
    
    // Alternative constructor for backward compatibility
    public TenantInfoResponse(String tenantId, List<String> options) {
        this.tenantId = tenantId;
        this.options = options;
    }
    
    // Helper method to check if options are empty
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
    
    // Helper method to get options count
    public int getOptionsCount() {
        return options != null ? options.size() : 0;
    }
} 