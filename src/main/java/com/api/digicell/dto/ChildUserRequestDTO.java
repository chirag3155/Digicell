package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Request DTO for child user list API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildUserRequestDTO {
    
    @Min(value = 1, message = "Page must be greater than 0")
    private int page = 1;
    
    @Min(value = 1, message = "Limit must be greater than 0")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private int limit = 10;
} 