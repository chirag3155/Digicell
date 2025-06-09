package com.api.digicell.services;

import com.api.digicell.dto.AgentCreateDTO;
import com.api.digicell.dto.AgentDetailsResponseDTO;
import com.api.digicell.dto.AgentStatusDTO;
import com.api.digicell.dto.AgentUpdateDTO;
import com.api.digicell.dto.ConversationResponseDTO;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.AgentStatus;
import com.api.digicell.entities.Conversation;
import com.api.digicell.exceptions.InvalidAgentStatusException;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.AgentRepository;
import com.api.digicell.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Create a new agent.
     * @throws InvalidAgentStatusException if the provided status is invalid
     */
    @Transactional
    public Agent createAgent(AgentCreateDTO createDTO) {
        logger.info("Creating new agent with name: {}", createDTO.getName());
        logger.debug("Agent creation request details - name: {}, status: {}, avatarUrl: {}, labels: {}", 
            createDTO.getName(), createDTO.getStatus(), createDTO.getAvatarUrl(), createDTO.getLabels());
        
        validateAgentStatus(createDTO.getStatus());
        
        Agent agent = new Agent();
        agent.setName(createDTO.getName());
        agent.setEmail(createDTO.getEmail());
        agent.setStatus(createDTO.getStatus());
        agent.setAvatarUrl(createDTO.getAvatarUrl());
        agent.setLabels(createDTO.getLabels());
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        
        Agent savedAgent = agentRepository.save(agent);
        logger.info("Successfully created agent with id: {}", savedAgent.getAgentId());
        logger.debug("Created agent details - id: {}, name: {}, status: {}, createdAt: {}", 
            savedAgent.getAgentId(), savedAgent.getName(), savedAgent.getStatus(), savedAgent.getCreatedAt());
        return savedAgent;
    }

    /**
     * Get all agents.
     * @throws RuntimeException if there's an error fetching agents
     */
    public List<Agent> getAllAgents() {
        logger.info("Fetching all agents");
        try {
            List<Agent> agents = agentRepository.findAll();
            logger.info("Successfully retrieved {} agents", agents.size());
            logger.debug("Retrieved agents - count: {}, ids: {}", 
                agents.size(), agents.stream().map(Agent::getAgentId).collect(Collectors.toList()));
            return agents;
        } catch (Exception e) {
            logger.error("Error fetching all agents: {}", e.getMessage());
            logger.info("Error fetching all agents: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch agents", e);
        }
    }

    /**
     * Get agent by ID.
     * @throws ResourceNotFoundException if agent is not found
     */
    public Agent getAgentById(Long id) {
        logger.info("Fetching agent with id: {}", id);
        return agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
    }

    /**
     * Update agent details.
     * @throws ResourceNotFoundException if agent is not found
     * @throws InvalidAgentStatusException if the provided status is invalid
     */
    @Transactional
    public Agent updateAgent(Long id, AgentUpdateDTO updateDTO) {
        logger.info("Updating agent with id: {}", id);
        logger.debug("Agent update request details - id: {}, name: {}, status: {}, avatarUrl: {}, labels: {}", 
            id, updateDTO.getName(), updateDTO.getStatus(), updateDTO.getAvatarUrl(), updateDTO.getLabels());
        
        validateAgentStatus(updateDTO.getStatus());
        
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for update with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        agent.setName(updateDTO.getName());
        agent.setStatus(updateDTO.getStatus());
        agent.setAvatarUrl(updateDTO.getAvatarUrl());
        agent.setLabels(updateDTO.getLabels());
        agent.setUpdatedAt(LocalDateTime.now());
        
        Agent updatedAgent = agentRepository.save(agent);
        logger.info("Successfully updated agent with id: {}", id);
        logger.debug("Updated agent details - id: {}, name: {}, status: {}, updatedAt: {}", 
            updatedAgent.getAgentId(), updatedAgent.getName(), updatedAgent.getStatus(), updatedAgent.getUpdatedAt());
        return updatedAgent;
    }

    /**
     * Update agent status.
     * @throws ResourceNotFoundException if agent is not found
     * @throws InvalidAgentStatusException if the provided status is invalid
     */
    @Transactional
    public Agent updateAgentStatus(Long id, AgentStatusDTO statusDTO) {
        logger.info("Updating status for agent with id: {}", id);
        logger.debug("Agent status update request - id: {}, new status: {}", id, statusDTO.getStatus());
        
        validateAgentStatus(statusDTO.getStatus());
        
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for status update with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        agent.setStatus(statusDTO.getStatus());
        agent.setUpdatedAt(LocalDateTime.now());
        Agent updatedAgent = agentRepository.save(agent);
        logger.info("Successfully updated agent status to: {} for agent id: {}", statusDTO.getStatus(), id);
        logger.debug("Updated agent status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, agent.getStatus(), updatedAgent.getStatus(), updatedAgent.getUpdatedAt());
        return updatedAgent;
    }

    /**
     * Delete agent.
     * @throws ResourceNotFoundException if agent is not found
     * @throws IllegalStateException if agent has active conversations
     */
    @Transactional
    public void deleteAgent(Long id) {
        logger.info("Attempting to delete agent with id: {}", id);
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for deletion with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        // Check if agent has any active conversations
        List<Conversation> activeConversations = conversationRepository.findByAgent_AgentId(id);
        if (!activeConversations.isEmpty()) {
            logger.error("Cannot delete agent with id: {} as they have {} active conversations", id, activeConversations.size());
            logger.info("Active conversations found for agent - id: {}, conversation count: {}", id, activeConversations.size());
            throw new IllegalStateException("Cannot delete agent with active conversations");
        }
        
        agentRepository.delete(agent);
        logger.info("Successfully deleted agent with id: {}", id);
        logger.debug("Deleted agent details - id: {}, name: {}, status: {}", 
            agent.getAgentId(), agent.getName(), agent.getStatus());
    }

    /**
     * Set agent status to AVAILABLE.
     * @throws ResourceNotFoundException if agent is not found
     */
    @Transactional
    public Agent setAgentAvailable(Long id) {
        logger.info("Setting agent with id: {} to AVAILABLE", id);
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for setting AVAILABLE status with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setUpdatedAt(LocalDateTime.now());
        Agent updatedAgent = agentRepository.save(agent);
        logger.info("Successfully set agent status to AVAILABLE for agent id: {}", id);
        logger.debug("Updated agent status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, agent.getStatus(), updatedAgent.getStatus(), updatedAgent.getUpdatedAt());
        return updatedAgent;
    }

    /**
     * Get agent details including conversations.
     * @throws ResourceNotFoundException if agent is not found
     * @throws RuntimeException if there's an error fetching details
     */
    @Transactional(readOnly = true)
    public AgentDetailsResponseDTO getAgentDetails(Long agentId) {
        logger.info("Fetching agent details for id: {}", agentId);
        try {
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> {
                        logger.error("Agent not found with id: {}", agentId);
                        logger.info("Failed to find agent for details with id: {}", agentId);
                        return new ResourceNotFoundException("Agent not found with id: " + agentId);
                    });

            List<Conversation> conversations = conversationRepository.findByAgent_AgentId(agentId);
            logger.info("Found {} conversations for agent {}", conversations.size(), agentId);
            logger.debug("Agent conversations details - id: {}, conversation count: {}, conversation ids: {}", 
                agentId, conversations.size(), conversations.stream().map(Conversation::getConversationId).collect(Collectors.toList()));

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
            logger.debug("Agent details response - id: {}, name: {}, status: {}, conversation count: {}", 
                agentId, agent.getName(), agent.getStatus(), conversationDTOs.size());
            return response;
        } catch (Exception e) {
            logger.error("Error fetching agent details for id {}: {}", agentId, e.getMessage());
            logger.info("Error details for agent id {}: {}", agentId, e.getMessage());
            throw new RuntimeException("Failed to fetch agent details", e);
        }
    }

    /**
     * Validates that the provided status is a valid AgentStatus enum value.
     * @throws InvalidAgentStatusException if the status is invalid
     */
    private void validateAgentStatus(AgentStatus status) {
        if (status == null) {
            logger.error("Agent status cannot be null");
            throw new InvalidAgentStatusException("null");
        }
        
        try {
            AgentStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid agent status: {}", status);
            logger.info("Invalid agent status details - status: {}", status);
            throw new InvalidAgentStatusException(status.name());
        }
    }

    @Transactional
    public Agent updateAgent(Agent agent) {
        return agentRepository.save(agent);
    }
} 