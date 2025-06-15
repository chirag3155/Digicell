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
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + dto.getClientId()));

        UserAccount userAccount = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found with id: " + dto.getUserId()));

        // Check for existing conversation
        Conversation existingConversation = conversationRepository.findByClientAndUserAccountAndEndTimeIsNull(client, userAccount)
            .orElse(null); // If no conversation exists, return null


        if (existingConversation != null) {
            // Update existing conversation
            existingConversation.setIntent(dto.getIntent());
            existingConversation.setEndTime(dto.getEndTime());
            if (dto.getChatHistory() != null) {
                existingConversation.setChatHistory(dto.getChatHistory());
            }
            conversationRepository.save(existingConversation);
            return existingConversation;
        }

        // Create new conversation if none exists
        Conversation conversation = new Conversation();
        conversation.setClient(client);
        conversation.setUserAccount(userAccount);
        conversation.setIntent(dto.getIntent());
        conversation.setStartTime(dto.getStartTime());
        conversation.setEndTime(dto.getEndTime());
        conversation.setChatHistory(dto.getChatHistory());
        conversationRepository.save(conversation);
        return conversation;
    }

    @Transactional
    public Conversation updateConversation(String id, Conversation updated) {
        Conversation existing = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
        existing.setChatHistory(updated.getChatHistory());
        existing.setEndTime(updated.getEndTime() != null ? updated.getEndTime() : LocalDateTime.now());
        return existing;
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
        // dto.setConversationId(conversation.getConversationId());
        dto.setUserId(conversation.getUserAccount().getUserId());
        dto.setUserName(conversation.getUserAccount().getUserName());
        dto.setIntent(conversation.getIntent());
        dto.setChatSummary(conversation.getChatSummary());
        
        // Convert chat history to the new format
        List<List<ChatHistoryDTO.MessageDTO>> formattedHistory = conversation.getChatHistory().stream()
            .map(messages -> messages.stream()
                .map(msg -> {
                    ChatHistoryDTO.MessageDTO messageDTO = new ChatHistoryDTO.MessageDTO();
                    // Format timestamp to match "2024-03-20T18:01:00" format
                    messageDTO.setTimestamp(msg.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    messageDTO.setContent(msg.getContent());
                    messageDTO.setRole(msg.getRole());
                    return messageDTO;
                })
                .collect(Collectors.toList()))  // Convert messages to MessageDTO                                                                                                                                                                                                                                                                                           
            .collect(Collectors.toList());
        // Set the formatted chat history       
        dto.setChatHistory(formattedHistory);
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
        
        dto.setIntent(conversation.getIntent());
        dto.setChatSummary(conversation.getChatSummary());
        dto.setChatHistory(conversation.getChatHistory());
        dto.setStartTime(conversation.getStartTime());
        dto.setEndTime(conversation.getEndTime());
        return dto;
    }
} 