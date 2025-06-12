package com.api.digicell.dto;

import com.api.digicell.entities.AgentStatus;
import lombok.Data;

@Data
public class AgentStatusDTO {
    private AgentStatus status;

    public AgentStatusDTO(AgentStatus status) {
        this.status = status;
    }
} 