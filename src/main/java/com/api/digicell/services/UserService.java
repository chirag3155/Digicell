package com.api.digicell.services;

import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.User;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.UserRepository;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.responses.UserDetailsResponse;
import com.api.digicell.dto.UserConversationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, ConversationRepository conversationRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public List<User> getUsersByAgent(Long agentId) {
        return userRepository.findActiveUsersByAgentId(agentId);
    }

    public List<User> getUsersByAssignmentStatus(boolean isAssigned) {
        return userRepository.findByIsAssigned(isAssigned);
    }

    /**
     * Returns a {@link UserDetailsResponse} containing the user as well as all their conversations.
     */
    public UserDetailsResponse getUserDetails(Long userId) {
        logger.info("Fetching user details for id: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

            List<Conversation> conversations = conversationRepository.findByUser_UserId(userId);
            logger.debug("Found {} conversations for user {}", conversations.size(), userId);

            List<UserConversationDTO> conversationDTOs = conversations.stream()
                    .map(conv -> {
                        UserConversationDTO dto = new UserConversationDTO();
                        dto.setConversationId(conv.getConversationId());
                        dto.setAgentId(conv.getAgent().getAgentId());
                        dto.setAgentName(conv.getAgent().getName());
                        dto.setStartTime(conv.getStartTime());
                        dto.setEndTime(conv.getEndTime());
                        dto.setQuery(conv.getQuery());
                        dto.setChatHistory(conv.getChatHistory());
                        return dto;
                    })
                    .collect(Collectors.toList());

            UserDetailsResponse response = new UserDetailsResponse();
            response.setUser(user);
            response.setConversations(conversationDTOs);

            logger.info("Successfully fetched user details for id: {}", userId);
            return response;
        } catch (Exception e) {
            logger.error("Error fetching user details for id {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    public List<UserConversationDTO> getUserConversations(Long userId) {
        logger.info("Fetching conversations for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<Conversation> conversations = conversationRepository.findByUser_UserId(userId);
        logger.debug("Found {} conversations for user {}", conversations.size(), userId);

        return conversations.stream()
                .map(conv -> {
                    UserConversationDTO dto = new UserConversationDTO();
                    dto.setConversationId(conv.getConversationId());
                    dto.setAgentId(conv.getAgent().getAgentId());
                    dto.setAgentName(conv.getAgent().getName());
                    dto.setStartTime(conv.getStartTime());
                    dto.setEndTime(conv.getEndTime());
                    dto.setQuery(conv.getQuery());
                    dto.setChatHistory(conv.getChatHistory());
                    return dto;
                })
                .collect(Collectors.toList());
    }
} 