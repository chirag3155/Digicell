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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {
    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Create a new agent.
     */
    @Transactional
    public Agent createAgent(AgentCreateDTO createDTO) {
        logger.info("Creating new agent with name: {}", createDTO.getName());
        logger.debug("Agent creation request details - name: {}, status: {}, avatarUrl: {}, labels: {}", 
            createDTO.getName(), createDTO.getStatus(), createDTO.getAvatarUrl(), createDTO.getLabels());
        
        validateAgentStatus(createDTO.getStatus());
        
        Agent agent = new Agent();
        agent.setName(createDTO.getName());
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
     */
    public List<Agent> getAllAgents() {
        logger.info("Fetching all agents");
        List<Agent> agents = agentRepository.findAll();
        logger.info("Successfully retrieved {} agents", agents.size());
        logger.debug("Retrieved agents - count: {}, ids: {}", 
            agents.size(), agents.stream().map(Agent::getAgentId).collect(Collectors.toList()));
        return agents;
    }

    /**
     * Get agent by ID.
     */
    public Agent getAgentById(Long id) {
        logger.info("Fetching agent with id: {}", id);
        return agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.debug("Failed to find agent with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
    }

    /**
     * Update agent details.
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
                    logger.debug("Failed to find agent for update with id: {}", id);
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
     */
    @Transactional
    public Agent updateAgentStatus(Long agentId, AgentStatusDTO statusDTO) {
        logger.info("Updating status for agent with id: {}", agentId);
        logger.debug("Agent status update request - id: {}, new status: {}", agentId, statusDTO.getStatus());
        
        validateAgentStatus(statusDTO.getStatus());
        
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setStatus(statusDTO.getStatus());
            agent.setUpdatedAt(LocalDateTime.now());
            return agentRepository.save(agent);
        }
        throw new RuntimeException("Agent not found with id: " + agentId);
    }

    /**
     * Delete agent.
     */
    @Transactional
    public void deleteAgent(Long id) {
        logger.info("Attempting to delete agent with id: {}", id);
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.debug("Failed to find agent for deletion with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        // Check if agent has any active conversations
        List<Conversation> activeConversations = conversationRepository.findByAgent_AgentId(id);
        if (!activeConversations.isEmpty()) {
            logger.error("Cannot delete agent with id: {} as they have {} active conversations", id, activeConversations.size());
            logger.debug("Active conversations found for agent - id: {}, conversation count: {}", id, activeConversations.size());
            throw new IllegalStateException("Cannot delete agent with active conversations");
        }
        
        agentRepository.delete(agent);
        logger.info("Successfully deleted agent with id: {}", id);
        logger.debug("Deleted agent details - id: {}, name: {}, status: {}", 
            agent.getAgentId(), agent.getName(), agent.getStatus());
    }

    /**
     * Set agent status to ONLINE.
     */
    @Transactional
    public Agent setAgentAvailable(Long agentId) {
        logger.info("Setting agent with id: {} to ONLINE", agentId);
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setStatus(AgentStatus.ONLINE);
            agent.setUpdatedAt(LocalDateTime.now());
            return agentRepository.save(agent);
        }
        throw new RuntimeException("Agent not found with id: " + agentId);
    }

    /**
     * Set agent status to OFFLINE.
     */
    @Transactional
    public Agent setAgentOffline(Long agentId) {
        logger.info("Setting agent with id: {} to OFFLINE", agentId);
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            agent.setStatus(AgentStatus.OFFLINE);
            agent.setUpdatedAt(LocalDateTime.now());
            return agentRepository.save(agent);
        }
        throw new RuntimeException("Agent not found with id: " + agentId);
    }

    /**
     * Get agent details including conversations.
     */
    @Transactional(readOnly = true)
    public AgentDetailsResponseDTO getAgentDetails(Long agentId) {
        logger.info("Fetching agent details for id: {}", agentId);
        try {
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> {
                        logger.error("Agent not found with id: {}", agentId);
                        logger.debug("Failed to find agent for details with id: {}", agentId);
                        return new IllegalArgumentException("Agent not found with id: " + agentId);
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
            logger.error("Error fetching agent details for id {}: {}", agentId, e.getMessage(), e);
            logger.debug("Error details for agent id {}: {}", agentId, e.getMessage());
            throw e;
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
            logger.debug("Invalid agent status details - status: {}", status);
            throw new InvalidAgentStatusException(status.name());
        }
    }

    public boolean canAgentGoOffline(Long agentId) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            // Add any additional checks here if needed
            return true;
        }
        return false;
    }
} 