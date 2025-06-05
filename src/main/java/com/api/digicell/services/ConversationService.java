package com.api.digicell.services;

import com.api.digicell.entities.Conversation;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

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

    public Conversation createConversation(Conversation conversation) {
        return conversationRepository.save(conversation);
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