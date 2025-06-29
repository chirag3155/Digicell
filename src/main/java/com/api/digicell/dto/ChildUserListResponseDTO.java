package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wrapper for the complete child user list API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildUserListResponseDTO {
    
    @JsonProperty("content")
    private List<ChildUserResponseDTO> content;
    
    @JsonProperty("pagination")
    private PaginationDTO pagination;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationDTO {
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