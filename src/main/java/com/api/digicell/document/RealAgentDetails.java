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
public class RealAgentDetails {
    
    @JsonProperty("agent_id")
    private String agentId;
    
    
    @JsonProperty("agent_name")
    private String agentName;
} 