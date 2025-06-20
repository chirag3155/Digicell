package com.api.digicell.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkFlow {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("customAttributes")
    private Object customAttributes;
} 