package com.api.digicell.controllers;

import com.api.digicell.dto.ConversationDTO;
import com.api.digicell.entities.Conversation;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.responses.ResponseUtil;
import com.api.digicell.services.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;

import com.api.digicell.dtos.ChatHistoryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CRUD and filter endpoints for Conversation entity.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Conversation Management", description = "APIs for managing conversations and chat history")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * List all conversations.
     */
    @Operation(
        summary = "Get all conversations",
        description = "Retrieves a list of all conversations in the system",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<Conversation>>> listAll(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        return ResponseUtil.listResponse(conversationService.getAllConversations(), "conversations");
    }

    /**
     * Get conversation by id.
     */
    @Operation(
        summary = "Get conversation by ID",
        description = "Retrieves a conversation by its unique ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{conversation_id}")
    public ResponseEntity<ApiResponse<Conversation>> getById(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId) {
        Conversation conv = conversationService.getConversationById(conversationId);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation fetched successfully", conv);
        return ResponseEntity.ok(response);
    }

    /**
     * Filter conversations by agent.
     */
    @Operation(
        summary = "Get conversations by agent",
        description = "Retrieves all conversations associated with a specific agent",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/agent/{agent_id}")
    public ResponseEntity<ApiResponse<List<Conversation>>> getByAgent(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId) {
        return ResponseUtil.listResponse(conversationService.getConversationsByAgent(agentId), "conversations for agent");
    }

    /**
     * Filter conversations by user.
     */
    @Operation(
        summary = "Get chat history by client ID",
        description = "Retrieves all chat history for a specific client",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/client/{client_id}")
    public ResponseEntity<ApiResponse<List<ChatHistoryDTO>>> getChatHistoryByUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("client_id") @Positive(message = "client_id must be positive") Long clientId) {
        List<ChatHistoryDTO> chatHistory = conversationService.getChatHistoryByUser(clientId);
        ApiResponse<List<ChatHistoryDTO>> response = new ApiResponse<>(
            HttpStatus.OK.value(),
            "Chat history retrieved successfully",
            chatHistory
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Filter conversations by agent and client.
     */
    @Operation(
        summary = "Get conversations by agent and client",
        description = "Retrieves conversations between a specific agent and client",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/agent/{agent_id}/client/{client_id}")
    public ResponseEntity<ApiResponse<List<Conversation>>> getByAgentAndUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId,
            @PathVariable("client_id") @Positive(message = "user_id must be positive") Long clientId) {
        return ResponseUtil.listResponse(conversationService.getConversationsByAgentAndUser(agentId, clientId), "conversations for agent and user");
    }

    /**
     * Create new conversation.
     */
    @Operation(
        summary = "Create a new conversation",
        description = "Creates a new conversation with the provided details",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Conversation>> create(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @Valid @RequestBody ConversationDTO dto) {
        Conversation created = conversationService.createConversation(dto);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Conversation created successfully", created);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update existing conversation.
     */
    @Operation(
        summary = "Update a conversation",
        description = "Updates an existing conversation with new details",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PutMapping("/{conversation_id}")
    public ResponseEntity<ApiResponse<Conversation>> update(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId,
            @Valid @RequestBody Conversation updated) {
        Conversation conv = conversationService.updateConversation(conversationId, updated);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation updated successfully", conv);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete conversation.
     */
    @Operation(
        summary = "Delete a conversation",
        description = "Deletes a conversation by its ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @DeleteMapping("/{conversation_id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId) {
        conversationService.deleteConversation(conversationId);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation with ID " + conversationId + " has been deleted", null);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get conversation details",
        description = "Retrieves detailed chat history for a specific conversation and user",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{conversation_id}/client/{client_id}")
    public ResponseEntity<ApiResponse<ChatHistoryDTO>> getConversationDetails(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId,
            @PathVariable("client_id") @Positive(message = "user_id must be positive") Long clientId) {
        ChatHistoryDTO conversation = conversationService.getConversationDetails(conversationId, clientId);
        ApiResponse<ChatHistoryDTO> response = new ApiResponse<>(
            HttpStatus.OK.value(),
            "Conversation details retrieved successfully",
            conversation
        );
        return ResponseEntity.ok(response);
    }

} 