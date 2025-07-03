package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * External API response mapping DTOs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUserResponseDTO {
    
    @JsonProperty("statusCode")
    private Integer statusCode;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("response")
    private ExternalUserDataDTO response;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUserDataDTO {
        
        @JsonProperty("content")
        private List<ExternalUserDTO> content;
        
        @JsonProperty("pagination")
        private ExternalPaginationDTO pagination;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalUserDTO {
        
        @JsonProperty("userId")
        private Long userId;
        
        @JsonProperty("userName")
        private String userName;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("phoneNumber")
        private String phoneNumber;
        
        @JsonProperty("status")
        private String status; // Keep as String to handle any external API format
        
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
        
        @JsonProperty("organizations")
        private List<Object> organizations; // We don't need organization details
        
        @JsonProperty("active")
        private Boolean active;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalPaginationDTO {
        
        @JsonProperty("currentPage")
        private Integer currentPage;
        
        @JsonProperty("totalItems")
        private Integer totalItems;
        
        @JsonProperty("totalPages")
        private Integer totalPages;
        
        @JsonProperty("nextPage")
        private Integer nextPage;
        
        @JsonProperty("previousPage")
        private Integer previousPage;
    }
} 