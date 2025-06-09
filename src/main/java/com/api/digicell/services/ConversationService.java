package com.api.digicell.services;

import com.api.digicell.dto.ConversationDTO;
import com.api.digicell.dtos.ChatHistoryDTO;
import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.User;
import com.api.digicell.entities.Agent;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.AgentRepository;
import com.api.digicell.repository.ConversationRepository;
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
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;

    public List<Conversation> getAllConversations() {
        return conversationRepository.findAll();
    }

    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    public List<Conversation> getConversationsByAgent(Long agentId) {
        return conversationRepository.findByAgent_AgentId(agentId);
    }

    public List<Conversation> getConversationsByUser(Long userId) {
        return conversationRepository.findByUser_UserId(userId);
    }

    public List<Conversation> getConversationsByAgentAndUser(Long agentId, Long userId) {
        return conversationRepository.findByAgent_AgentIdAndUser_UserId(agentId, userId);
    }

    @Transactional
    public Conversation createConversation(ConversationDTO dto) {
        User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + dto.getUserId()));
        
        Agent agent = agentRepository.findById(dto.getAgentId())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found with id: " + dto.getAgentId()));

        // Check for existing conversation
        Conversation existingConversation = conversationRepository.findByUserAndAgentAndEndTimeIsNull(user, agent)
            .orElse(null); // If no conversation exists, return null


        if (existingConversation != null) {
            // Update existing conversation
            existingConversation.setQuery(dto.getQuery());
            existingConversation.setEndTime(dto.getEndTime());
            if (dto.getChatHistory() != null) {
                existingConversation.setChatHistory(dto.getChatHistory());
            }
            conversationRepository.save(existingConversation);
            return existingConversation;
        }

        // Create new conversation if none exists
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setAgent(agent);
        conversation.setQuery(dto.getQuery());
        conversation.setStartTime(dto.getStartTime());
        conversation.setEndTime(dto.getEndTime());
        conversation.setChatHistory(dto.getChatHistory());
        conversationRepository.save(conversation);
        return conversation;
    }

    @Transactional
    public Conversation updateConversation(Long id, Conversation updated) {
        Conversation existing = getConversationById(id);
        existing.setChatHistory(updated.getChatHistory());
        existing.setEndTime(updated.getEndTime() != null ? updated.getEndTime() : LocalDateTime.now());
        return existing;
    }

    public void deleteConversation(Long id) {
        if (!conversationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + id);
        }
        conversationRepository.deleteById(id);
    }

    public List<ChatHistoryDTO> getChatHistoryByUser(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUser_UserId(userId);
        return conversations.stream()
                .map(this::convertToChatHistoryDTO)
                .collect(Collectors.toList());
    }

    public ChatHistoryDTO getConversationDetails(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByConversationIdAndUser_UserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return convertToChatHistoryDTO(conversation);
    }

    private ChatHistoryDTO convertToChatHistoryDTO(Conversation conversation) {
        ChatHistoryDTO dto = new ChatHistoryDTO();
        dto.setAgentId(conversation.getAgent().getAgentId());
        dto.setAgentName(conversation.getAgent().getName());
        dto.setQuery(conversation.getQuery());
        
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
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
        
        dto.setChatHistory(formattedHistory);
        return dto;
    }
} 