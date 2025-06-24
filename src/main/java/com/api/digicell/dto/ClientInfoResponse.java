package com.api.digicell.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfoResponse {
    private String status;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("client_name")
    private String clientName;
    
    @JsonProperty("client_label")
    private String clientLabel;

    @JsonProperty("client_email")
    private String clientEmail;

    @JsonProperty("client_phone")
    private String clientPhone;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("history")
    private String history;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("client_id")
    private String clientId;


    

}
