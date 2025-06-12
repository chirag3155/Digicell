package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentCreateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String avatarUrl;
    
    private List<String> labels;
    
    @NotNull(message = "Status is required")
    @Builder.Default
    private AgentStatus status = AgentStatus.ONLINE;
} 