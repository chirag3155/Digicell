package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.api.digicell.entities.UserAccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for child user with Redis status information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildUserResponseDTO {
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("userName")
    private String userName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    private UserAccountStatus status;
    
    @JsonProperty("createdBy")
    private String createdBy;
    
    @JsonProperty("updatedBy")
    private String updatedBy;
    
    @JsonProperty("isAgent")
    private Boolean isAgent;
    
    @JsonProperty("createdAt")
    private String createdAt;
    
    @JsonProperty("updatedAt")
    private String updatedAt;
    
    @JsonProperty("active")
    private Boolean active;
    
    // Redis-specific fields
    @JsonProperty("redisStatus")
    private String redisStatus; // "Active" or "Inactive"
    
    @JsonProperty("activeClients")
    private List<String> activeClients; // List of client IDs from active conversations
}

 