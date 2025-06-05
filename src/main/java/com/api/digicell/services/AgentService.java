package com.api.digicell.services;

import com.api.digicell.dto.AgentDetailsResponseDTO;
import com.api.digicell.dto.ConversationResponseDTO;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.Conversation;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.AgentRepository;
import com.api.digicell.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;

    public List<Agent> getAllAgents() {
        logger.info("Fetching all agents");
        List<Agent> agents = agentRepository.findAll();
        logger.debug("Found {} agents", agents.size());
        return agents;
    }

    public Agent getAgentById(Long id) {
        logger.info("Fetching agent with id: {}", id);
        return agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public AgentDetailsResponseDTO getAgentDetails(Long agentId) {
        logger.info("Fetching agent details for id: {}", agentId);
        try {
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found with id: " + agentId));

            List<Conversation> conversations = conversationRepository.findByAgent_AgentId(agentId);
            logger.debug("Found {} conversations for agent {}", conversations.size(), agentId);

            List<ConversationResponseDTO> conversationDTOs = conversations.stream()
                    .map(conv -> {
                        ConversationResponseDTO dto = new ConversationResponseDTO();
                        dto.setConversationId(conv.getConversationId());
                        dto.setUserId(conv.getUser().getUserId());
                        dto.setUserName(conv.getUser().getName());
                        dto.setStartTime(conv.getStartTime());
                        dto.setEndTime(conv.getEndTime());
                        dto.setQuery(conv.getQuery());
                        dto.setChatHistory(conv.getChatHistory());
                        return dto;
                    })
                    .collect(Collectors.toList());

            AgentDetailsResponseDTO response = new AgentDetailsResponseDTO();
            response.setAgent(agent);
            response.setConversations(conversationDTOs);

            logger.info("Successfully fetched agent details for id: {}", agentId);
            return response;
        } catch (Exception e) {
            logger.error("Error fetching agent details for id {}: {}", agentId, e.getMessage(), e);
            throw e;
        }
    }
} 