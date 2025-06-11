package com.api.digicell.services;

import com.api.digicell.entities.Client;
import com.api.digicell.entities.Conversation;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.repository.ClientRepository;
import com.api.digicell.repository.ConversationRepository;
import com.api.digicell.responses.ClientDetailsResponse;
import com.api.digicell.dto.ClientConvoDto;
import com.api.digicell.dto.ConvoDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class ClientService {
    private final ClientRepository clientRepository;
    private final ConversationRepository conversationRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    public ClientService(ClientRepository clientRepository, ConversationRepository conversationRepository) {
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    public List<Client> getClientsByAgent(Long agentId) {
        return clientRepository.findByAgent_AgentId(agentId);
    }

    public List<Client> getClientsByAssignmentStatus(boolean isAssigned) {
        return clientRepository.findByIsAssigned(isAssigned);
    }

    /**
     * Returns a {@link ClientDetailsResponse} containing the client as well as all their conversations.
     */
    public ClientDetailsResponse getClientDetails(Long clientId) {
        logger.info("Fetching client details for id: {}", clientId);
        try {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + clientId));

            List<Conversation> conversations = conversationRepository.findByClient_ClientId(clientId);
            logger.debug("Found {} conversations for client {}", conversations.size(), clientId);

            List<ClientConvoDto> conversationDTOs = conversations.stream()
                    .map(conv -> {
                        ClientConvoDto dto = new ClientConvoDto();
                        dto.setConversationId(conv.getConversationId());
                        dto.setAgentId(conv.getUserAccount().getAgentId());
                        dto.setAgentName(conv.getUserAccount().getName());
                        dto.setStartTime(conv.getStartTime());
                        dto.setEndTime(conv.getEndTime());
                        dto.setIntent(conv.getIntent());
                        dto.setChatSummary(conv.getChatSummary());
                        dto.setChatHistory(conv.getChatHistory());
                        return dto;
                    })
                    .collect(Collectors.toList());

            ClientDetailsResponse response = new ClientDetailsResponse();
            response.setClient(client);
            response.setConversations(conversationDTOs);

            logger.info("Successfully fetched client details for id: {}", clientId);
            return response;
        } catch (Exception e) {
            logger.error("Error fetching client details for id {}: {}", clientId, e.getMessage(), e);
            throw e;
        }
    }

    public List<ConvoDto> getClientConversations(Long clientId) {
        logger.info("Fetching conversations for client ID: {}", clientId);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + clientId));

        List<Conversation> conversations = conversationRepository.findByClient_ClientId(clientId);
        logger.debug("Found {} conversations for client {}", conversations.size(), clientId);

        return conversations.stream()
                .map(conv -> {
                    ConvoDto dto = new ConvoDto();
                    dto.setConversationId(conv.getConversationId());
                    dto.setAgentId(conv.getUserAccount().getAgentId());
                    dto.setAgentName(conv.getUserAccount().getName());
                    dto.setStartTime(conv.getStartTime());
                    dto.setEndTime(conv.getEndTime());
                    dto.setIntent(conv.getIntent());
                    dto.setChatSummary(conv.getChatSummary());
                    return dto;
                })
                .collect(Collectors.toList());
    }
} 