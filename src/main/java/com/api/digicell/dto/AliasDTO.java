package com.api.digicell.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AliasDTO {
    @NotBlank(message = "Key is required")
    private String key;
    
    @NotBlank(message = "Value is required")
    private String value;
} 