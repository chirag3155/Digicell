package com.api.digicell.controllers;

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

/**
 * CRUD and filter endpoints for Conversation entity.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Validated
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
    public ResponseEntity<ApiResponse<Conversation>> getById(@PathVariable("conversation_id") @Positive(message = "conversation_id must be positive") Long conversationId) {
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
    public ResponseEntity<ApiResponse<List<Conversation>>> getByUser(@PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        return ResponseUtil.listResponse(conversationService.getConversationsByUser(userId), "conversations for user");
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
    public ResponseEntity<ApiResponse<Conversation>> create(@Valid @RequestBody Conversation conversation) {
        Conversation created = conversationService.createConversation(conversation);
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
} 