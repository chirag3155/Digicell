package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentUpdateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String avatarUrl;
    
    private List<String> labels;
    
    @NotNull(message = "Status is required")
    private AgentStatus status;
} 