package com.api.digicell.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.api.digicell.document.ConversationDocument;
import com.api.digicell.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class ConversationElasticService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationElasticService.class);
    private final Optional<ElasticsearchClient> esClient;

    @Value("${elasticsearch.index.name}")
    private String indexName; // This will inject the value from application.properties

    // Constructor with lazy Elasticsearch client injection
    public ConversationElasticService(@Lazy Optional<ElasticsearchClient> esClient) {
        this.esClient = esClient;
    }

    public List<ConversationDocument> getConversationsByUser(Long userId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID: " + userId);
            }

            logger.info("Searching for conversations with agent_id: {} using wildcard pattern: {}*", userId, indexName);

            // Build search request with wildcard index pattern and robust error handling
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName + "*") // Use wildcard pattern to search multiple indices
                    .ignoreUnavailable(true) // Skip indices that don't exist
                    .allowNoIndices(true) // Allow the query even if no indices match the pattern
                    .ignoreThrottled(true) // Skip throttled indices
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.match(m -> m
                                    .field("messages.system_response.type")
                                    .query("Real Agent")))
                            .must(QueryBuilders.exists(e -> e
                                    .field("messages.system_response.real_agent_details")))
                            .must(QueryBuilders.match(m -> m
                                    .field("messages.system_response.real_agent_details.agent_id")
                                    .query(userId.toString())))
                    ))
                    .build();

            // Execute search with detailed error handling
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            logger.info("Search executed successfully across indices {}*. Total hits: {}", indexName, searchResponse.hits().total().value());

            // Log detailed shard information
            if (searchResponse.shards() != null) {
                logger.info("Search executed across {} shards, {} successful, {} failed", 
                    searchResponse.shards().total(), 
                    searchResponse.shards().successful(), 
                    searchResponse.shards().failed());
                
                // Log any shard failures (but don't fail the request)
                if (searchResponse.shards().failed().intValue() > 0) {
                    logger.warn("Some shards failed during search. This may be due to index structure differences. Continuing with available results.");
                }
            }

            // Extract hits with error handling for structure mismatches
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(hit -> {
                        try {
                            logger.debug("Processing conversation from index: {} with id: {}", hit.index(), hit.id());
                            ConversationDocument conversation = hit.source();
                            
                            if (conversation == null) {
                                logger.warn("Null conversation found in index: {} with id: {}", hit.index(), hit.id());
                                return null;
                            }
                            
                            // Validate basic structure
                            if (conversation.getConversationId() == null) {
                                logger.warn("Conversation missing ID in index: {}, skipping", hit.index());
                                return null;
                            }
                            
                            return conversation;
                            
                        } catch (Exception e) {
                            logger.warn("Error processing conversation from index: {} with id: {}. Error: {}. Skipping this document.", 
                                    hit.index(), hit.id(), e.getMessage());
                            return null; // Skip documents that can't be processed
                        }
                    })
                    .filter(conversation -> conversation != null) // Filter out failed documents
                    .collect(Collectors.toList());

            logger.info("Successfully processed {} conversations for agent_id: {} from indices matching {}*", 
                    conversations.size(), userId, indexName);
            
            return conversations;

        } catch (IOException e) {
            logger.error("Elasticsearch IO error for agent_id {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error searching for agent_id {}: {}", userId, e.getMessage(), e);
            
            // Handle specific error types gracefully
            String errorMessage = e.getMessage().toLowerCase();
            if (errorMessage.contains("mapping") || errorMessage.contains("field") || 
                errorMessage.contains("parsing") || errorMessage.contains("no such field")) {
                logger.warn("Index structure mismatch detected. Some indices may not have the expected field structure. Returning empty results.");
                return new ArrayList<>(); // Return empty list instead of throwing
            }
            
            if (errorMessage.contains("index_not_found") || errorMessage.contains("no such index")) {
                logger.warn("No matching indices found for pattern: {}*. Returning empty results.", indexName);
                return new ArrayList<>();
            }
            
            // For other unexpected errors, still throw
            throw new RuntimeException("Failed to retrieve conversations for agent: " + userId, e);
        }
    }

    public List<ConversationDocument> getConversationsByClient(String clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            // Build search request - searching for human user (client)
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(QueryBuilders.match(q -> q.field("user_info.id").query(clientId))) // Search by user_info.id (human client)
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for human client: " + clientId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        }
    }

    public List<ConversationDocument> getConversationsByUserAndClient(Long userId, String clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID: " + userId);
            }
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            logger.info("Searching for conversations with agent_id: {} and client_id: {} using wildcard pattern: {}*", userId, clientId, indexName);

            // Build search request with wildcard index pattern and graceful error handling
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName + "*") // Use wildcard pattern to search multiple indices
                    .ignoreUnavailable(true) // Skip indices that don't exist
                    .allowNoIndices(true) // Allow the query even if no indices match the pattern
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.match(m -> m
                                    .field("user_info.id")
                                    .query(clientId))) // Human client
                            .must(QueryBuilders.match(m -> m
                                    .field("messages.system_response.type")
                                    .query("Real Agent")))
                            .must(QueryBuilders.exists(e -> e
                                    .field("messages.system_response.real_agent_details")))
                            .must(QueryBuilders.match(m -> m
                                    .field("messages.system_response.real_agent_details.agent_id")
                                    .query(userId.toString())))
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            logger.info("Search executed successfully across indices {}*. Total hits: {}", indexName, searchResponse.hits().total().value());

            // Log which indices were searched
            if (searchResponse.shards() != null) {
                logger.info("Search executed across {} shards, {} successful, {} failed", 
                    searchResponse.shards().total(), 
                    searchResponse.shards().successful(), 
                    searchResponse.shards().failed());
            }

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(hit -> {
                        logger.debug("Found conversation in index: {} with id: {}", hit.index(), hit.id());
                        return hit.source();
                    })
                    .filter(conversation -> conversation != null) // Filter out any null results
                    .collect(Collectors.toList());

            logger.info("Returning {} conversations for agent_id: {} and client_id: {} from indices matching {}*", conversations.size(), userId, clientId, indexName);
            return conversations;

        } catch (IOException e) {
            logger.error("Error querying Elasticsearch for agent_id {} and client_id {}: {}", userId, clientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error searching for agent_id {} and client_id {}: {}", userId, clientId, e.getMessage(), e);
            // If it's a mapping exception, log it but don't fail completely
            if (e.getMessage().contains("mapping") || e.getMessage().contains("field")) {
                logger.warn("Some indices may not have the expected field structure. Continuing with available results.");
                return new ArrayList<>(); // Return empty list instead of throwing
            }
            throw new RuntimeException("Failed to retrieve conversations for agent: " + userId + " and client: " + clientId, e);
        }
    }

    public ConversationDocument getConversationByIdAndClient(String conversationId, String clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (conversationId == null || conversationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid conversation ID: " + conversationId);
            }

            if (clientId == null || clientId.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            // Build search request - search by conversationId and human client
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.term(t -> t.field("convesation_id.keyword").value(conversationId)))
                            .must(QueryBuilders.term(t -> t.field("user_info.id.keyword").value(clientId))) // Human client
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Return the first match or throw exception if not found
            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId + " and client id: " + clientId));
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversation: " + e.getMessage());
        }
    }

    public ConversationDocument createConversation(ConversationDocument conversation) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (conversation == null) {
                throw new IllegalArgumentException("Conversation cannot be null");
            }
            if (conversation.getUserInfo() == null || conversation.getUserInfo().getId() == null) {
                throw new IllegalArgumentException("User info and user ID must not be null");
            }

            // Create the index request to add the conversation to Elasticsearch
            IndexRequest<ConversationDocument> indexRequest = new IndexRequest.Builder<ConversationDocument>()
                    .index("complete-conversation-2025.06.20") // Elasticsearch index name
                    .id(conversation.getConversationId()) // Use conversation_id as document ID
                    .document(conversation) // The document to be indexed
                    .build();

            // Execute the indexing request
            IndexResponse response = esClient.get().index(indexRequest);

            // Log response and return the conversation if successful
            logger.info("Conversation created with ID: {}", response.id());
            return conversation;

        } catch (IOException e) {
            logger.error("Error creating conversation: {}", e.getMessage());
            throw new RuntimeException("Failed to create conversation: " + e.getMessage());
        }
    }

    /**
     * Get all conversations that involved real agents (not just AI system)
     */
    public List<ConversationDocument> getConversationsWithRealAgents() {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            // Build search request - searching for conversations with real agent responses
            // Double nested query: messages[] -> system_response[] -> type
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(QueryBuilders.nested(n -> n
                            .path("messages")
                            .query(QueryBuilders.nested(nested -> nested
                                    .path("messages.system_response")
                                    .query(QueryBuilders.term(t -> t.field("messages.system_response.type").value("Real Agent")))
                            ))
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations with real agents: " + e.getMessage());
        }
    }
}
