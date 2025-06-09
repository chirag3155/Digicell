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
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CRUD and filter endpoints for Conversation entity.
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Conversation", description = "Conversation management APIs")
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * List all conversations.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Conversation>>> listAll() {
        return ResponseUtil.listResponse(conversationService.getAllConversations(), "conversations");
    }

    /**
     * Get conversation by id.
     */
    @GetMapping("/{conversation_id}")
    @Operation(summary = "Get conversation by ID", description = "Retrieves a conversation by its ID")
    public ResponseEntity<ApiResponse<Conversation>> getById(
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId) {
        Conversation conv = conversationService.getConversationById(conversationId);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation fetched successfully", conv);
        return ResponseEntity.ok(response);
    }

    /**
     * Filter conversations by agent.
     */
    @GetMapping("/agent/{agent_id}")
    public ResponseEntity<ApiResponse<List<Conversation>>> getByAgent(@PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId) {
        return ResponseUtil.listResponse(conversationService.getConversationsByAgent(agentId), "conversations for agent");
    }

    /**
     * Filter conversations by user.
     */
    @GetMapping("/user/{user_id}")
    @Operation(summary = "Get chat history by user ID", description = "Retrieves all chat history for a specific user")
    public ResponseEntity<ApiResponse<List<ChatHistoryDTO>>> getChatHistoryByUser(
            @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        List<ChatHistoryDTO> chatHistory = conversationService.getChatHistoryByUser(userId);
        ApiResponse<List<ChatHistoryDTO>> response = new ApiResponse<>(
            HttpStatus.OK.value(),
            "Chat history retrieved successfully",
            chatHistory
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Filter conversations by agent and user.
     */
    @GetMapping("/agent/{agent_id}/user/{user_id}")
    public ResponseEntity<ApiResponse<List<Conversation>>> getByAgentAndUser(@PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId,
                                                                             @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        return ResponseUtil.listResponse(conversationService.getConversationsByAgentAndUser(agentId, userId), "conversations for agent and user");
    }

    /**
     * Create new conversation.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Conversation>> create(@Valid @RequestBody ConversationDTO dto) {
        Conversation created = conversationService.createConversation(dto);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Conversation created successfully", created);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update existing conversation.
     */
    @PutMapping("/{conversation_id}")
    public ResponseEntity<ApiResponse<Conversation>> update(@PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId,
                                                             @Valid @RequestBody Conversation updated) {
        Conversation conv = conversationService.updateConversation(conversationId, updated);
        ApiResponse<Conversation> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation updated successfully", conv);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete conversation.
     */
    @DeleteMapping("/{conversation_id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId) {
        conversationService.deleteConversation(conversationId);
        ApiResponse<Void> response = new ApiResponse<>(HttpStatus.OK.value(), "Conversation with ID " + conversationId + " has been deleted", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversation_id}/user/{user_id}")
    @Operation(summary = "Get conversation details", description = "Retrieves detailed chat history for a specific conversation and user")
    public ResponseEntity<ApiResponse<ChatHistoryDTO>> getConversationDetails(
            @PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId,
            @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        ChatHistoryDTO conversation = conversationService.getConversationDetails(conversationId, userId);
        ApiResponse<ChatHistoryDTO> response = new ApiResponse<>(
            HttpStatus.OK.value(),
            "Conversation details retrieved successfully",
            conversation
        );
        return ResponseEntity.ok(response);
    }

} 