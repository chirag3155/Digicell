package com.api.digicell.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessageResponse {
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("client_id")
    private String clientId;
    
    private String message;
    
    private String timestamp;
} 