package com.api.digicell.dto;

import com.api.digicell.entities.Agent;
import lombok.Data;
import java.util.List;

@Data
public class AgentDetailsResponseDTO {
    private Agent agent;
    private List<ConversationResponseDTO> conversations;
} 