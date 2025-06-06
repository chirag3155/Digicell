package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusDTO {
    @NotNull(message = "Status is required")
    private AgentStatus status;
} 