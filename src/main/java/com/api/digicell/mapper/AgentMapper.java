package com.api.digicell.mapper;

import com.api.digicell.dto.AgentUpdateDTO;
import com.api.digicell.entities.Agent;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public void updateAgentFromDTO(Agent agent, AgentUpdateDTO dto) {
        if (dto.getName() != null) {
            agent.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            agent.setEmail(dto.getEmail());
        }
        if (dto.getAvatarUrl() != null) {
            agent.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getLabels() != null) {
            agent.setLabels(dto.getLabels());
        }
        if (dto.getStatus() != null) {
            agent.setStatus(dto.getStatus());
        }
        agent.setUpdatedAt(java.time.LocalDateTime.now());
    }
} 