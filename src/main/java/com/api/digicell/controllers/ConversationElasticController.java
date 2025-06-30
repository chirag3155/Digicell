package com.api.digicell.controllers;

import com.api.digicell.document.ConversationDocument;
import com.api.digicell.services.ConversationElasticService;
import com.api.digicell.utils.ApiResponse;
import com.api.digicell.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/elastic/conversation/")
@RequiredArgsConstructor
@Tag(name = "Conversation Elastic", description = "APIs for managing conversations in Elasticsearch")
@SecurityRequirement(name = "bearerAuth")
public class ConversationElasticController {
   private static final Logger logger = LoggerFactory.getLogger(ConversationElasticController.class);
   private final ConversationElasticService conversationElasticService;

  @Operation(
        summary = "Create a new conversation",
        description = "Creates a new conversation document in Elasticsearch",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
  @PostMapping
  public ResponseEntity<ApiResponse<ConversationDocument>> createConversation(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @RequestBody ConversationDocument conversation) {
      try {
          logger.info("Creating new conversation for client: {} and session: {}",
              conversation.getUserInfo() != null ? conversation.getUserInfo().getId() : "unknown", 
              conversation.getSessionId());

          ConversationDocument createdConversation = conversationElasticService.createConversation(conversation);

          ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(true, "Conversation created successfully", createdConversation);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      } catch (Exception e) {
          logger.error("Error creating conversation: {}", e.getMessage());
          ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(false, "Failed to create conversation: " + e.getMessage(), null);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
  }

   @Operation(
        summary = "Get conversations by user",
        description = "Retrieves all conversations for a specific user/agent",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
   @GetMapping("/user/{userId}")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable Long userId) {
    
       try {
           logger.info("Retrieving conversations for agent: {}", userId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByUser(userId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for user {}: {}", userId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @Operation(
        summary = "Get conversations by client",
        description = "Retrieves all conversations for a specific client",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
   @GetMapping("/client/{clientId}")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByClient(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable String clientId) {
       try {
           logger.info("Retrieving conversations for human client: {}", clientId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByClient(clientId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for client {}: {}", clientId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @Operation(
        summary = "Get conversations by user and client",
        description = "Retrieves all conversations between a specific user/agent and client",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
   @GetMapping("/user/{userId}/client/{clientId}")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByUserAndClient(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable Long userId,
            @PathVariable String clientId) {
       try {
           logger.info("Retrieving conversations for AI system: {} and human client: {}", userId, clientId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByUserAndClient(userId, clientId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for user {} and client {}: {}", userId, clientId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @Operation(
        summary = "Get conversation by ID and client",
        description = "Retrieves a specific conversation by ID for a specific client",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
   @GetMapping("/{conversationId}/client/{clientId}")
   public ResponseEntity<ApiResponse<ConversationDocument>> getConversationByIdAndClient(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable String conversationId,
            @PathVariable String clientId) {
       try {
           logger.info("Retrieving conversation: {} for client: {}", conversationId, clientId);
           ConversationDocument conversation = conversationElasticService.getConversationByIdAndClient(conversationId, clientId);
           ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(true, "Conversation retrieved successfully", conversation);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversation {} for client {}: {}", conversationId, clientId, e.getMessage());
           ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(false, "Failed to retrieve conversation: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @Operation(
        summary = "Get conversations with real agents",
        description = "Retrieves all conversations that were handled by real agents (not AI)",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
   @GetMapping("/real-agents")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsWithRealAgents(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
       try {
           logger.info("Retrieving conversations handled by real agents");
           List<ConversationDocument> conversations = conversationElasticService.getConversationsWithRealAgents();
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Real agent conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving real agent conversations: {}", e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve real agent conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

}