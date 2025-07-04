package com.api.digicell.controllers;

import com.api.digicell.dto.ConvoDto;
import com.api.digicell.entities.Client;
import com.api.digicell.services.ClientService;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.responses.ClientDetailsResponse;
import com.api.digicell.responses.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Validated
public class ClientController {
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    private final ClientService clientService;

    /**
     * List all clients.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Client>>> listAllClients() {
        logger.info("Received request to list all clients");
        try {
            List<Client> clients = clientService.getAllClients();
            logger.debug("Found {} clients", clients.size());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Clients fetched successfully", clients));
        } catch (Exception e) {
            logger.error("Error fetching clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching clients", null));
        }
    }

    /**
     * Get client by id.
     */
    @GetMapping("/{client_id}")
    public ResponseEntity<ApiResponse<Client>> getClientById(@PathVariable("client_id") @Positive(message = "client_id must be positive") Long clientId) {
        Client client = clientService.getClientById(clientId);
        ApiResponse<Client> response = new ApiResponse<>(HttpStatus.OK.value(), "Client fetched successfully", client);
        return ResponseEntity.ok(response);
    }

    /**
     * Get clients filtered by assignment status.
     */
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<Client>>> listClientsByAssignment(@RequestParam("status") boolean status) {
        List<Client> clients = clientService.getClientsByAssignmentStatus(status);
        return ResponseUtil.listResponse(clients, status? "assigned clients" : "unassigned clients");
    }

    /**
     * Fetch a client along with all conversation details.
     */
    @GetMapping("/{client_id}/details")
    public ResponseEntity<ApiResponse<ClientDetailsResponse>> getClientDetails(
            @PathVariable("client_id") @Positive(message = "client_id must be positive") Long clientId) {
        logger.info("Received request to get details for client: {}", clientId);
        try {
            ClientDetailsResponse details = clientService.getClientDetails(clientId);
            logger.debug("Successfully fetched details for client: {}", clientId);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Client details fetched successfully", details));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid client ID provided: {}", clientId);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching client details for id {}: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching client details", null));
        }
    }

    @GetMapping("/{client_id}/conversations")
    public ResponseEntity<ApiResponse<List<ConvoDto>>> getClientConversations(
            @PathVariable("client_id") @Positive(message = "client_id must be positive") Long clientId) {
        logger.info("Received request to get conversations for client: {}", clientId);
        try {
            List<ConvoDto> conversations = clientService.getClientConversations(clientId);
            logger.debug("Found {} conversations for client: {}", conversations.size(), clientId);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Client conversations fetched successfully", conversations));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid client ID provided: {}", clientId);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching conversations for client {}: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching client conversations", null));
        }
    }
} 