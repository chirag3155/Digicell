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

import java.util.List;

@RestController
@RequestMapping("/api/v1/elastic/conversations")
@RequiredArgsConstructor
public class ConversationElasticController {
   private static final Logger logger = LoggerFactory.getLogger(ConversationElasticController.class);
   private final ConversationElasticService conversationElasticService;

  @PostMapping
  public ResponseEntity<ApiResponse<ConversationDocument>> createConversation(@RequestBody ConversationDocument conversation) {
      try {
          logger.info("Creating new conversation for user: {} and client: {}",
              conversation.getUserId(), conversation.getClientId());

          ConversationDocument createdConversation = conversationElasticService.createConversation(conversation);

          ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(true, "Conversation created successfully", createdConversation);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      } catch (Exception e) {
          logger.error("Error creating conversation: {}", e.getMessage());
          ApiResponse<ConversationDocument> response = new ApiResponse<ConversationDocument>(false, "Failed to create conversation: " + e.getMessage(), null);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
  }

   @GetMapping("/user/{userId}")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByUser(@PathVariable Long userId) {
    
       try {
           logger.info("Retrieving conversations for user: {}", userId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByUser(userId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for user {}: {}", userId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @GetMapping("/client/{clientId}")
       public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByClient(@PathVariable String clientId) {
       try {
           logger.info("Retrieving conversations for client: {}", clientId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByClient(clientId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for client {}: {}", clientId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @GetMapping("/user/{userId}/client/{clientId}")
   public ResponseEntity<ApiResponse<List<ConversationDocument>>> getConversationsByUserAndClient(
           @PathVariable Long userId,
           @PathVariable String clientId) {
       try {
           logger.info("Retrieving conversations for user: {} and client: {}", userId, clientId);
           List<ConversationDocument> conversations = conversationElasticService.getConversationsByUserAndClient(userId, clientId);
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(true, "Conversations retrieved successfully", conversations);
           return ResponseEntity.ok(response);
       } catch (Exception e) {
           logger.error("Error retrieving conversations for user {} and client {}: {}", userId, clientId, e.getMessage());
           ApiResponse<List<ConversationDocument>> response = new ApiResponse<List<ConversationDocument>>(false, "Failed to retrieve conversations: " + e.getMessage(), null);
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       }
   }

   @GetMapping("/{conversationId}/client/{clientId}")
   public ResponseEntity<ApiResponse<ConversationDocument>> getConversationByIdAndClient(
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
}