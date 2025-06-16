package com.api.digicell.services;

import com.api.digicell.dto.ConDTO;
import com.api.digicell.dto.ConversationDTO;
import com.api.digicell.dto.UserAccountResponseDTO;
import com.api.digicell.dtos.ChatHistoryDTO;
import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.repository.ClientRepository;

import com.api.digicell.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public List<ConDTO> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream()
                .map(this::convertToConDTO)
                .collect(Collectors.toList());
    }

    public ConDTO getConversationById(String id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
        return convertToConDTO(conversation);
    }

    public List<ConDTO> getConversationsByUser(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserAccount_UserId(userId);
        return conversations.stream()
                .map(this::convertToConDTO)
                .collect(Collectors.toList());
    }

    public List<ConDTO> getConversationsByUserAndClient(Long userId, String clientId) {
        List<Conversation> conversations = conversationRepository.findByUserAccount_UserIdAndClient_ClientId(userId, clientId);
        return conversations.stream()
                .map(this::convertToConDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Conversation createConversation(ConversationDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + dto.getClientId()));

        UserAccount userAccount = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + dto.getUserId()));

        // Check for existing conversation
        Optional<Conversation> existingConversation = conversationRepository.findByClientAndUserAccount(client, userAccount);

        if (existingConversation.isPresent()) {
            // Return existing conversation
            return existingConversation.get();
        }

        // Create new conversation if none exists
        Conversation conversation = new Conversation();
        // Use the conversationId from the DTO (from EVENT_AGENT_REQUEST)
        if (dto.getConversationId() != null && !dto.getConversationId().trim().isEmpty()) {
            conversation.setConversationId(dto.getConversationId());
        } else {
            throw new IllegalArgumentException("ConversationId is required and cannot be null or empty");
        }
        conversation.setClient(client);
        conversation.setUserAccount(userAccount);
        conversationRepository.save(conversation);
        return conversation;
    }

    @Transactional
    public Conversation updateConversation(String id, Conversation updated) {
        Conversation existing = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
        
        // Only update the fields that exist in the simplified entity
        // The conversation entity now only has conversationId, client, and userAccount
        // No other fields to update
        return conversationRepository.save(existing);
    }

    public void deleteConversation(String id) {
        if (!conversationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + id);
        }
        conversationRepository.deleteById(id);
    }

    public List<ChatHistoryDTO> getChatHistoryByClient(String clientId) {
        List<Conversation> conversations = conversationRepository.findByClient_ClientId(clientId);
        return conversations.stream()
                .map(this::convertToChatHistoryDTO)
                .collect(Collectors.toList());
    }

    public ChatHistoryDTO getConversationDetails(String conversationId, String clientId) {
        Conversation conversation = conversationRepository.findByConversationIdAndClient_ClientId(conversationId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return convertToChatHistoryDTO(conversation);
    }

    private ChatHistoryDTO convertToChatHistoryDTO(Conversation conversation) {
        ChatHistoryDTO dto = new ChatHistoryDTO();
        // Note: The dtos.ChatHistoryDTO doesn't have conversationId field, so we skip it
        dto.setUserId(conversation.getUserAccount().getUserId());
        dto.setUserName(conversation.getUserAccount().getUserName());
        
        // Since the conversation entity no longer has intent, chatSummary, or chatHistory fields,
        // we'll set default values or leave them null
        dto.setIntent("SUPPORT"); // Default value
        dto.setChatSummary("Chat conversation"); // Default value
        dto.setChatHistory(null); // No chat history available in simplified entity
        
        return dto;
    }

    private ConDTO convertToConDTO(Conversation conversation) {
        ConDTO dto = new ConDTO();
        dto.setClient(conversation.getClient());
        
        UserAccount userAccount = conversation.getUserAccount();
        UserAccountResponseDTO userDTO = new UserAccountResponseDTO(
            userAccount.getUserId(),
            userAccount.getUserName(),
            userAccount.getEmail(),
            userAccount.getPhoneNumber(),
            userAccount.isActive(),
            userAccount.getStatus(),
            userAccount.getCreatedBy(),
            userAccount.getCreatedAt(),
            userAccount.getUpdatedAt()
        );
        dto.setUserAccountResponseDTO(userDTO);
        
        // Since the conversation entity no longer has these fields, set default values
        dto.setIntent("SUPPORT"); // Default value
        dto.setChatSummary("Chat conversation"); // Default value
        dto.setChatHistory(null); // No chat history available in simplified entity
        dto.setStartTime(null); // No start time in simplified entity
        dto.setEndTime(null); // No end time in simplified entity
        return dto;
    }
} 