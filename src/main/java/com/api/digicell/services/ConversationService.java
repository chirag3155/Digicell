package com.api.digicell.services;

import com.api.digicell.dto.ConversationDTO;
import com.api.digicell.entities.Conversation;
import com.api.digicell.entities.Client;
import com.api.digicell.entities.Agent;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.repository.ClientRepository;
import com.api.digicell.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;
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

    public List<Conversation> getConversationsByClient(Long clientId) {
        return conversationRepository.findByClient_ClientId(clientId);
    }

    public List<Conversation> getConversationsByAgentAndClient(Long agentId, Long clientId) {
        return conversationRepository.findByAgent_AgentIdAndClient_ClientId(agentId, clientId);
    }

    @Transactional
    public Conversation createConversation(ConversationDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
            .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + dto.getClientId()));
        
        Agent agent = agentRepository.findById(dto.getAgentId())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found with id: " + dto.getAgentId()));

        // Check for existing conversation
        Conversation existingConversation = conversationRepository.findByClientAndAgentAndEndTimeIsNull(client, agent)
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
        conversation.setClient(client);
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
} 