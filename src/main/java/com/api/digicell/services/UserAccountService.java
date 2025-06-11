package com.api.digicell.services;

import com.api.digicell.dto.UserAccountCreateDTO;
import com.api.digicell.dto.UserAccountDetailsResponseDTO;
import com.api.digicell.dto.UserAccountStatusDTO;
import com.api.digicell.dto.UserAccountUpdateDTO;
import com.api.digicell.dto.ConversationResponseDTO;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserAccountStatus;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private static final Logger logger = LoggerFactory.getLogger(UserAccountService.class);
    private final AgentRepository agentRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Create a new agent.
     * @throws InvalidAgentStatusException if the provided status is invalid
     */
    @Transactional
    public UserAccount createAgent(UserAccountCreateDTO createDTO) {
        logger.info("Creating new agent with name: {}", createDTO.getName());
        logger.debug("Agent creation request details - name: {}, status: {}, avatarUrl: {}, labels: {}", 
            createDTO.getName(), createDTO.getStatus(), createDTO.getAvatarUrl(), createDTO.getLabels());
        
        validateAgentStatus(createDTO.getStatus());
        
        UserAccount userAccount = new UserAccount();
        userAccount.setUserName(createDTO.getName());
        userAccount.setEmail(createDTO.getEmail());
        userAccount.setStatus(createDTO.getStatus());
        userAccount.setLabels(createDTO.getLabels());
        userAccount.setCreatedAt(LocalDateTime.now());
        userAccount.setUpdatedAt(LocalDateTime.now());
        
        UserAccount savedUserAccount = agentRepository.save(userAccount);
        logger.info("Successfully created agent with id: {}", savedUserAccount.getUserId());
        logger.debug("Created agent details - id: {}, name: {}, status: {}, createdAt: {}", 
            savedUserAccount.getUserId(), savedUserAccount.getUserName(), savedUserAccount.getStatus(), savedUserAccount.getCreatedAt());
        return savedUserAccount;
    }

    /**
     * Get all agents.
     * @throws RuntimeException if there's an error fetching agents
     */
    public List<UserAccount> getAllAgents() {
        logger.info("Fetching all agents");
        try {
            List<UserAccount> userAccounts = agentRepository.findAll();
            logger.info("Successfully retrieved {} agents", userAccounts.size());
            logger.debug("Retrieved agents - count: {}, ids: {}", 
                userAccounts.size(), userAccounts.stream().map(UserAccount::getUserId).collect(Collectors.toList()));
            return userAccounts;
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
    public UserAccount getAgentById(Long id) {
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
    public UserAccount updateAgent(Long id, UserAccountUpdateDTO updateDTO) {
        logger.info("Updating agent with id: {}", id);
        logger.debug("Agent update request details - id: {}, name: {}, status: {}, avatarUrl: {}, labels: {}", 
            id, updateDTO.getName(), updateDTO.getStatus(), updateDTO.getAvatarUrl(), updateDTO.getLabels());
        
        validateAgentStatus(updateDTO.getStatus());
        
        UserAccount userAccount = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for update with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        userAccount.setUserName(updateDTO.getName());
        userAccount.setStatus(updateDTO.getStatus());
        userAccount.setLabels(updateDTO.getLabels());
        userAccount.setUpdatedAt(LocalDateTime.now());
        
        UserAccount updatedUserAccount = agentRepository.save(userAccount);
        logger.info("Successfully updated agent with id: {}", id);
        logger.debug("Updated agent details - id: {}, name: {}, status: {}, updatedAt: {}", 
            updatedUserAccount.getAgentId(), updatedUserAccount.getName(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Update agent status.
     * @throws ResourceNotFoundException if agent is not found
     * @throws InvalidAgentStatusException if the provided status is invalid
     */
    @Transactional
    public UserAccount updateAgentStatus(Long id, UserAccountStatusDTO statusDTO) {
        logger.info("Updating status for agent with id: {}", id);
        logger.debug("Agent status update request - id: {}, new status: {}", id, statusDTO.getStatus());
        
        validateAgentStatus(statusDTO.getStatus());
        
        UserAccount userAccount = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for status update with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        userAccount.setStatus(statusDTO.getStatus());
        userAccount.setUpdatedAt(LocalDateTime.now());
        UserAccount updatedUserAccount = agentRepository.save(userAccount);
        logger.info("Successfully updated agent status to: {} for agent id: {}", statusDTO.getStatus(), id);
        logger.debug("Updated agent status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, userAccount.getStatus(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Delete agent.
     * @throws ResourceNotFoundException if agent is not found
     * @throws IllegalStateException if agent has active conversations
     */
    @Transactional
    public void deleteAgent(Long id) {
        logger.info("Attempting to delete agent with id: {}", id);
        UserAccount userAccount = agentRepository.findById(id)
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
        
        agentRepository.delete(userAccount);
        logger.info("Successfully deleted agent with id: {}", id);
        logger.debug("Deleted agent details - id: {}, name: {}, status: {}", 
            userAccount.getAgentId(), userAccount.getName(), userAccount.getStatus());
    }

    /**
     * Set agent status to AVAILABLE.
     * @throws ResourceNotFoundException if agent is not found
     */
    @Transactional
    public UserAccount setAgentAvailable(Long id) {
        logger.info("Setting agent with id: {} to AVAILABLE", id);
        UserAccount userAccount = agentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Agent not found with id: {}", id);
                    logger.info("Failed to find agent for setting AVAILABLE status with id: {}", id);
                    return new ResourceNotFoundException("Agent not found with id: " + id);
                });
        
        userAccount.setStatus(UserAccountStatus.AVAILABLE);
        userAccount.setUpdatedAt(LocalDateTime.now());
        UserAccount updatedUserAccount = agentRepository.save(userAccount);
        logger.info("Successfully set agent status to AVAILABLE for agent id: {}", id);
        logger.debug("Updated agent status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, userAccount.getStatus(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Get agent details including conversations.
     * @throws ResourceNotFoundException if agent is not found
     * @throws RuntimeException if there's an error fetching details
     */
    @Transactional(readOnly = true)
    public UserAccountDetailsResponseDTO getAgentDetails(Long agentId) {
        logger.info("Fetching agent details for id: {}", agentId);
        try {
            UserAccount userAccount = agentRepository.findById(agentId)
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
                        dto.setClientId(conv.getClient().getClientId());
                        dto.setUserName(conv.getClient().getName());
                        dto.setStartTime(conv.getStartTime());
                        dto.setEndTime(conv.getEndTime());
                        dto.setIntent(conv.getIntent());
                        dto.setChatHistory(conv.getChatHistory());
                        return dto;
                    })
                    .collect(Collectors.toList());

            UserAccountDetailsResponseDTO response = new UserAccountDetailsResponseDTO();
            response.setUserAccount(userAccount);
            response.setConversations(conversationDTOs);

            logger.info("Successfully fetched agent details for id: {}", agentId);
            logger.debug("Agent details response - id: {}, name: {}, status: {}, conversation count: {}", 
                agentId, userAccount.getName(), userAccount.getStatus(), conversationDTOs.size());
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
    private void validateAgentStatus(UserAccountStatus status) {
        if (status == null) {
            logger.error("Agent status cannot be null");
            throw new InvalidAgentStatusException("null");
        }
        
        try {
            UserAccountStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid agent status: {}", status);
            logger.info("Invalid agent status details - status: {}", status);
            throw new InvalidAgentStatusException(status.name());
        }
    }

    @Transactional
    public UserAccount updateAgent(UserAccount userAccount) {
        return agentRepository.save(userAccount);
    }
} 