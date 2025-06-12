package com.api.digicell.services;

import com.api.digicell.dto.UserAccountCreateDTO;
import com.api.digicell.dto.UserAccountDetailsResponseDTO;
import com.api.digicell.dto.UserAccountResponseDTO;
import com.api.digicell.dto.UserAccountStatusDTO;
import com.api.digicell.dto.UserAccountUpdateDTO;
import com.api.digicell.dto.ConversationResponseDTO;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.UserAccountStatus;
import com.api.digicell.entities.Conversation;
import com.api.digicell.exceptions.InvalidUserStatusException;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    /**
     * Create a new user.
     * @throws InvalidUserStatusException if the provided status is invalid
     */
    @Transactional
    public UserAccount createUser(UserAccountCreateDTO createDTO) {
        logger.info("Creating new user with name: {}", createDTO.getName());
        logger.debug("user creation request details - name: {}, status: {}, avatarUrl: {}, labels: {}", 
            createDTO.getName(), createDTO.getStatus(), createDTO.getAvatarUrl(), createDTO.getLabels());
        
        validateuserStatus(createDTO.getStatus());
        
        UserAccount userAccount = new UserAccount();
        userAccount.setUserName(createDTO.getName());
        userAccount.setEmail(createDTO.getEmail());
        userAccount.setStatus(createDTO.getStatus());
        userAccount.setLabels(createDTO.getLabels());
        userAccount.setCreatedAt(LocalDateTime.now());
        userAccount.setUpdatedAt(LocalDateTime.now());
        
        UserAccount savedUserAccount = userRepository.save(userAccount);
        logger.info("Successfully created user with id: {}", savedUserAccount.getUserId());
        logger.debug("Created user details - id: {}, name: {}, status: {}, createdAt: {}", 
            savedUserAccount.getUserId(), savedUserAccount.getUserName(), savedUserAccount.getStatus(), savedUserAccount.getCreatedAt());
        return savedUserAccount;
    }

    /**
     * Get all users.
     * @throws RuntimeException if there's an error fetching users
     */
    public List<UserAccount> getAllUsers() {
        logger.info("Fetching all users");
        try {
            List<UserAccount> userAccounts = userRepository.findAll();
            logger.info("Successfully retrieved {} users", userAccounts.size());
            logger.debug("Retrieved users - count: {}, ids: {}", 
                userAccounts.size(), userAccounts.stream().map(UserAccount::getUserId).collect(Collectors.toList()));
            return userAccounts;
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage());
            logger.info("Error fetching all users: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    /**
     * Get user by ID.
     * @throws ResourceNotFoundException if user is not found
     */
    public UserAccount getUserById(Long id) {
        logger.info("Fetching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("user not found with id: {}", id);
                    logger.info("Failed to find user with id: {}", id);
                    return new ResourceNotFoundException("user not found with id: " + id);
                });
    }

    /**
     * Update user details.
     * @throws ResourceNotFoundException if user is not found
     * @throws InvalidUserStatusException if the provided status is invalid
     */
    @Transactional
    public UserAccount updateUser(Long id, UserAccountUpdateDTO updateDTO) {
        logger.info("Updating user with id: {}", id);
        logger.debug("user update request details - id: {}, name: {}, status: {}, avatarUrl: {}, labels: {}", 
            id, updateDTO.getName(), updateDTO.getStatus(), updateDTO.getAvatarUrl(), updateDTO.getLabels());
        
        validateuserStatus(updateDTO.getStatus());
        
        UserAccount userAccount = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("user not found with id: {}", id);
                    logger.info("Failed to find user for update with id: {}", id);
                    return new ResourceNotFoundException("user not found with id: " + id);
                });
        
        userAccount.setUserName(updateDTO.getName());
        userAccount.setStatus(updateDTO.getStatus());
        userAccount.setLabels(updateDTO.getLabels());
        userAccount.setUpdatedAt(LocalDateTime.now());
        
        UserAccount updatedUserAccount = userRepository.save(userAccount);
        logger.info("Successfully updated user with id: {}", id);
        logger.debug("Updated user details - id: {}, name: {}, status: {}, updatedAt: {}", 
            updatedUserAccount.getUserId(), updatedUserAccount.getUserName(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Update user status.
     * @throws ResourceNotFoundException if user is not found
     * @throws InvalidUserStatusException if the provided status is invalid
     */
    @Transactional
    public UserAccount updateUserStatus(Long id, UserAccountStatusDTO statusDTO) {
        logger.info("Updating status for user with id: {}", id);
        logger.debug("user status update request - id: {}, new status: {}", id, statusDTO.getStatus());
        
        validateuserStatus(statusDTO.getStatus());
        
        UserAccount userAccount = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("user not found with id: {}", id);
                    logger.info("Failed to find user for status update with id: {}", id);
                    return new ResourceNotFoundException("user not found with id: " + id);
                });
        
        userAccount.setStatus(statusDTO.getStatus());
        userAccount.setUpdatedAt(LocalDateTime.now());
        UserAccount updatedUserAccount = userRepository.save(userAccount);
        logger.info("Successfully updated user status to: {} for user id: {}", statusDTO.getStatus(), id);
        logger.debug("Updated user status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, userAccount.getStatus(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Delete user.
     * @throws ResourceNotFoundException if user is not found
     * @throws InvalidUserStatusException if user has active conversations
     */
    @Transactional
    public void deleteUser(Long id) {
        logger.info("Attempting to delete user with id: {}", id);
        UserAccount userAccount = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("user not found with id: {}", id);
                    logger.info("Failed to find user for deletion with id: {}", id);
                    return new ResourceNotFoundException("user not found with id: " + id);
                });
        
        // Check if user has any active conversations
        List<Conversation> activeConversations = conversationRepository.findByUserAccount_UserId(id);
        // if (!activeConversations.isEmpty()) {
        //     logger.error("Cannot delete user with id: {} as they have {} active conversations", id, activeConversations.size());
        //     logger.info("Active conversations found for user - id: {}, conversation count: {}", id, activeConversations.size());
        //     throw new InvalidUserStatusException("Cannot delete user with active conversations");
        // }
        conversationRepository.deleteAll(activeConversations);
        userRepository.delete(userAccount);
        logger.info("Successfully deleted user with id: {}", id);
        logger.debug("Deleted user details - id: {}, name: {}, status: {}", 
            userAccount.getUserId(), userAccount.getUserName(), userAccount.getStatus());
    }

    /**
     * Set user status to AVAILABLE.
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional
    public UserAccount setUserAvailable(Long id) {
        logger.info("Setting user with id: {} to AVAILABLE", id);
        UserAccount userAccount = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("user not found with id: {}", id);
                    logger.info("Failed to find user for setting AVAILABLE status with id: {}", id);
                    return new ResourceNotFoundException("user not found with id: " + id);
                });
        
        userAccount.setStatus(UserAccountStatus.AVAILABLE);
        userAccount.setUpdatedAt(LocalDateTime.now());
        UserAccount updatedUserAccount = userRepository.save(userAccount);
        logger.info("Successfully set user status to AVAILABLE for user id: {}", id);
        logger.debug("Updated user status details - id: {}, old status: {}, new status: {}, updatedAt: {}", 
            id, userAccount.getStatus(), updatedUserAccount.getStatus(), updatedUserAccount.getUpdatedAt());
        return updatedUserAccount;
    }

    /**
     * Get user details including conversations.
     * @throws ResourceNotFoundException if user is not found
     * @throws RuntimeException if there's an error fetching details
     */
    @Transactional(readOnly = true)
    public UserAccountDetailsResponseDTO getUserDetails(Long userId) {
        logger.info("Fetching user details for id: {}", userId);
        try {
            UserAccount userAccount = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("user not found with id: {}", userId);
                        logger.info("Failed to find user for details with id: {}", userId);
                        return new ResourceNotFoundException("user not found with id: " + userId);
                    });

            List<Conversation> conversations = conversationRepository.findByUserAccount_UserId(userId);
            logger.info("Found {} conversations for user {}", conversations.size(), userId);
            logger.debug("user conversations details - id: {}, conversation count: {}, conversation ids: {}", 
                userId, conversations.size(), conversations.stream().map(Conversation::getConversationId).collect(Collectors.toList()));

            List<ConversationResponseDTO> conversationDTOs = conversations.stream()
                    .map(conv -> {
                        ConversationResponseDTO dto = new ConversationResponseDTO();
                        dto.setConversationId(conv.getConversationId());
                        dto.setClientId(conv.getClient().getClientId());
                        dto.setUserName(conv.getClient().getName());
                        dto.setStartTime(conv.getStartTime());
                        dto.setEndTime(conv.getEndTime());
                        dto.setIntent(conv.getIntent());
                        dto.setChatSummary(conv.getChatSummary());
                        dto.setChatHistory(conv.getChatHistory());
                        return dto;
                    })
                    .collect(Collectors.toList());

            UserAccountResponseDTO userAccountResponseDTO = new UserAccountResponseDTO();
            userAccountResponseDTO.setUserId(userAccount.getUserId());
            userAccountResponseDTO.setUserName(userAccount.getUserName());
            userAccountResponseDTO.setEmail(userAccount.getEmail());
            userAccountResponseDTO.setPhoneNumber(userAccount.getPhoneNumber());
            userAccountResponseDTO.setActive(userAccount.isActive());
            userAccountResponseDTO.setLabels(userAccount.getLabels());
            userAccountResponseDTO.setStatus(userAccount.getStatus());
            userAccountResponseDTO.setCreatedBy(userAccount.getCreatedBy());
            userAccountResponseDTO.setCreatedAt(userAccount.getCreatedAt()); 
            userAccountResponseDTO.setUpdatedAt(userAccount.getUpdatedAt());

            UserAccountDetailsResponseDTO response = new UserAccountDetailsResponseDTO();
            response.setUserAccountResponseDTO(userAccountResponseDTO);
            response.setConversations(conversationDTOs);

            logger.info("Successfully fetched user details for id: {}", userId);
            logger.debug("user details response - id: {}, name: {}, status: {}, conversation count: {}", 
                userId, userAccount.getUserName(), userAccount.getStatus(), conversationDTOs.size());
            return response;
        } catch (Exception e) {
            logger.error("Error fetching user details for id {}: {}", userId, e.getMessage());
            logger.info("Error details for user id {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to fetch user details", e);
        }
    }

    /**
     * Validates that the provided status is a valid userStatus enum value.
     * @throws InvalidUserStatusException if the status is invalid
     */
    private void validateuserStatus(UserAccountStatus status) {
        if (status == null) {
            logger.error("user status cannot be null");
            throw new InvalidUserStatusException("null");
        }
        
        try {
            UserAccountStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid user status: {}", status);
            logger.info("Invalid user status details - status: {}", status);
            throw new InvalidUserStatusException(status.name());
        }
    }

    @Transactional
    public UserAccount updateuser(UserAccount userAccount) {
        return userRepository.save(userAccount);
    }
} 