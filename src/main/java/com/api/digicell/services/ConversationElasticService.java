 package com.api.digicell.services;

 import co.elastic.clients.elasticsearch.ElasticsearchClient;
 import com.api.digicell.document.ConversationDocument;
 import com.api.digicell.document.ChatMessageDocument;
 import com.api.digicell.entities.Conversation;
 import com.api.digicell.exceptions.ResourceNotFoundException;
 import com.api.digicell.repository.elasticsearch.ConversationElasticRepository;
 import lombok.RequiredArgsConstructor;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.util.List;
 import java.util.stream.Collectors;

 @Service
 @RequiredArgsConstructor
 public class ConversationElasticService {
     private static final Logger logger = LoggerFactory.getLogger(ConversationElasticService.class);
     private final ConversationElasticRepository conversationElasticRepository;

     private final ElasticsearchClient elasticsearchClient; // This is injected from your ElasticsearchConfig

     private boolean isElasticsearchAvailable() {
         // Check if the Elasticsearch client is available (not null)
         return elasticsearchClient != null;
     }

     public List<ConversationDocument> getConversationsByUser(Long userId) {
         if (!isElasticsearchAvailable()) {
             logger.warn("Elasticsearch is not available. Cannot fetch conversations for user {}", userId);
             throw new RuntimeException("Elasticsearch is not available.");
         }
         try {
             if (userId == null || userId <= 0) {
                 throw new IllegalArgumentException("Invalid user ID: " + userId);
             }
             List<ConversationDocument> conversations = conversationElasticRepository.findByUserId(userId);
             if (conversations.isEmpty()) {
                 throw new ResourceNotFoundException("No conversations found for user: " + userId);
             }
             return conversations;
         } catch (ResourceNotFoundException e) {
             throw e;
         } catch (Exception e) {
             logger.error("Error fetching conversations for user {}: {}", userId, e.getMessage());
             throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
         }
     }

     public List<ConversationDocument> getConversationsByClient(Long clientId) {
         if (!isElasticsearchAvailable()) {
             logger.warn("Elasticsearch is not available. Cannot fetch conversations for client {}", clientId);
             throw new RuntimeException("Elasticsearch is not available.");
         }
         try {
             if (clientId == null || clientId <= 0) {
                 throw new IllegalArgumentException("Invalid client ID: " + clientId);
             }
             List<ConversationDocument> conversations = conversationElasticRepository.findByClientId(clientId);
             if (conversations.isEmpty()) {
                 throw new ResourceNotFoundException("No conversations found for client: " + clientId);
             }
             return conversations;
         } catch (ResourceNotFoundException e) {
             throw e;
         } catch (Exception e) {
             logger.error("Error fetching conversations for client {}: {}", clientId, e.getMessage());
             throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
         }
     }

     public List<ConversationDocument> getConversationsByUserAndClient(Long userId, Long clientId) {
         if (!isElasticsearchAvailable()) {
             logger.warn("Elasticsearch is not available. Cannot fetch conversations for user {} and client {}", userId, clientId);
             throw new RuntimeException("Elasticsearch is not available.");
         }
         try {
             if (userId == null || userId <= 0) {
                 throw new IllegalArgumentException("Invalid user ID: " + userId);
             }
             if (clientId == null || clientId <= 0) {
                 throw new IllegalArgumentException("Invalid client ID: " + clientId);
             }
             List<ConversationDocument> conversations = conversationElasticRepository.findByUserIdAndClientId(userId, clientId);
             if (conversations.isEmpty()) {
                 throw new ResourceNotFoundException("No conversations found for user: " + userId + " and client: " + clientId);
             }
             return conversations;
         } catch (ResourceNotFoundException e) {
             throw e;
         } catch (Exception e) {
             logger.error("Error fetching conversations for user {} and client {}: {}", userId, clientId, e.getMessage());
             throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
         }
     }

     public ConversationDocument getConversationByIdAndClient(Long conversationId, Long clientId) {
         if (!isElasticsearchAvailable()) {
             logger.warn("Elasticsearch is not available. Cannot fetch conversation with ID {} and client {}", conversationId, clientId);
             throw new RuntimeException("Elasticsearch is not available.");
         }
         try {
             if (conversationId == null || conversationId <= 0) {
                 throw new IllegalArgumentException("Invalid conversation ID: " + conversationId);
             }
             if (clientId == null || clientId <= 0) {
                 throw new IllegalArgumentException("Invalid client ID: " + clientId);
             }
             return conversationElasticRepository.findByConversationIdAndClientId(conversationId, clientId)
                     .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId + " and client id: " + clientId));
         } catch (ResourceNotFoundException e) {
             throw e;
         } catch (Exception e) {
             logger.error("Error fetching conversation {} for client {}: {}", conversationId, clientId, e.getMessage());
             throw new RuntimeException("Failed to fetch conversation: " + e.getMessage());
         }
     }

     @Transactional
     public ConversationDocument createConversation(ConversationDocument conversation) {
         if (!isElasticsearchAvailable()) {
             logger.warn("Elasticsearch is not available. Cannot create conversation.");
             throw new RuntimeException("Elasticsearch is not available.");
         }
         try {
             if (conversation == null) {
                 throw new IllegalArgumentException("Conversation cannot be null");
             }
             if (conversation.getUserId() == null || conversation.getClientId() == null) {
                 throw new IllegalArgumentException("User ID and client ID must not be null");
             }

             return conversationElasticRepository.save(conversation);
         } catch (Exception e) {
             logger.error("Error creating conversation: {}", e.getMessage());
             throw new RuntimeException("Failed to create conversation: " + e.getMessage());
         }
     }

 }