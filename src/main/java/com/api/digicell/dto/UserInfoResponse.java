package com.api.digicell.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String status;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("agent_name")
    private String userName;
    
    @JsonProperty("agent_label")
    private String userLabel;
} 