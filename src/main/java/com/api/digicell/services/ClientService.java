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
import java.util.Set;

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

    public Client getClientById(String id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    public List<Client> getClientsByUser(Long userId) {
        return clientRepository.findByUserAccount_UserId(userId);
    }

    public List<Client> getClientsByAssignmentStatus(boolean isAssigned) {
        return clientRepository.findByIsAssigned(isAssigned);
    }

    /**
     * Returns a list of clients that have the specified label.
     * @param label The label to search for
     * @return List of clients with the specified label
     */
    public List<Client> getClientsByLabel(String label) {
        logger.info("Fetching clients with label: {}", label);
        try {
            List<Client> clients = clientRepository.findAll().stream()
                .filter(client -> client.getLabels() != null && client.getLabels().contains(label))
                .collect(Collectors.toList());
            
            logger.debug("Found {} clients with label {}", clients.size(), label);
            return clients;
        } catch (Exception e) {
            logger.error("Error fetching clients with label {}: {}", label, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Returns a list of clients that have any of the specified labels.
     * @param labels The list of labels to search for
     * @return List of clients that have any of the specified labels
     * @throws IllegalArgumentException if labels list is null or empty
     */
    public List<Client> getClientsByLabels(List<String> labels) {
        logger.info("Fetching clients with labels: {}", labels);
        
        // Edge case 1: Null or empty labels list
        if (labels == null || labels.isEmpty()) {
            logger.warn("Labels list is null or empty");
            throw new IllegalArgumentException("Labels list cannot be null or empty");
        }


        try {
            Set<String> labelSet = Set.copyOf(labels);
            List<Client> clients = clientRepository.findAll().stream()
                .filter(client -> client.getLabels() != null && 
                       client.getLabels().stream().anyMatch(labelSet::contains))
                .collect(Collectors.toList());
            
            // Edge case 3: No clients found
            if (clients.isEmpty()) {
                logger.info("No clients found with labels: {}", labels);
            } else {
                logger.debug("Found {} clients with labels {}", clients.size(), labels);
            }
            
            return clients;
        } catch (Exception e) {
            logger.error("Error fetching clients with labels {}: {}", labels, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Returns a {@link ClientDetailsResponse} containing the client as well as all their conversations.
     */
    public ClientDetailsResponse getClientDetails(String clientId) {
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
                        dto.setUserId(conv.getUserAccount().getUserId());
                        dto.setUserName(conv.getUserAccount().getUserName());
                        // Set default values for removed fields
                        dto.setStartTime(null);
                        dto.setEndTime(null);
                        dto.setIntent("SUPPORT");
                        dto.setChatSummary("Chat conversation");
                        dto.setChatHistory(null);
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

    public List<ConvoDto> getClientConversations(String clientId) {
        logger.info("Fetching conversations for client ID: {}", clientId);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + clientId));

        List<Conversation> conversations = conversationRepository.findByClient_ClientId(clientId);
        logger.debug("Found {} conversations for client {}", conversations.size(), clientId);

        return conversations.stream()
                .map(conv -> {
                    ConvoDto dto = new ConvoDto();
                    dto.setConversationId(conv.getConversationId());
                    dto.setUserId(conv.getUserAccount().getUserId());
                    dto.setUserName(conv.getUserAccount().getUserName());
                    // Set default values for removed fields
                    dto.setStartTime(null);
                    dto.setEndTime(null);
                    dto.setIntent("SUPPORT");
                    dto.setChatSummary("Chat conversation");
                    return dto;
                })
                .collect(Collectors.toList());
    }
} 