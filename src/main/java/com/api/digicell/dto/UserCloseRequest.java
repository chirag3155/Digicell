package com.api.digicell.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCloseRequest {
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("conversationId")
    private String conversationId;
    
    @JsonProperty("clientId")
    private String clientId;
    
    private String timestamp;
} 