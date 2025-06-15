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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationElasticService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationElasticService.class);
    private final Optional<ElasticsearchClient> esClient;

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

            // Build search request
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("conversations")
                    .query(QueryBuilders.term(q -> q.field("userId").value(userId))) // Elasticsearch query for userId
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for user: " + userId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        }
    }

    public List<ConversationDocument> getConversationsByClient(Long clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (clientId == null || clientId <= 0) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            // Build search request
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("conversations")
                    .query(QueryBuilders.term(q -> q.field("clientId").value(clientId))) // Elasticsearch query for clientId
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for client: " + clientId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        }
    }

    public List<ConversationDocument> getConversationsByUserAndClient(Long userId, Long clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID: " + userId);
            }
            if (clientId == null || clientId <= 0) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            // Build search request
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("conversations")
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.term(t -> t.field("userId").value(userId)))
                            .must(QueryBuilders.term(t -> t.field("clientId").value(clientId)))
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for user: " + userId + " and client: " + clientId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
        }
    }

    public ConversationDocument getConversationByIdAndClient(Long conversationId, Long clientId) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (conversationId == null || conversationId <= 0) {
                throw new IllegalArgumentException("Invalid conversation ID: " + conversationId);
            }

            if (clientId == null || clientId <= 0) {
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            // Build search request
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("conversations")
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.term(t -> t.field("conversationId").value(conversationId)))
                            .must(QueryBuilders.term(t -> t.field("clientId").value(clientId)))
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

    // For creating conversation, you can add the manual save method, though we focus on retrieval here.

    public ConversationDocument createConversation(ConversationDocument conversation) {
        if (esClient.isEmpty()) {
            logger.warn("Elasticsearch client is not available");
            throw new RuntimeException("Elasticsearch is not available");
        }

        try {
            if (conversation == null) {
                throw new IllegalArgumentException("Conversation cannot be null");
            }
            if (conversation.getUserId() == null || conversation.getClientId() == null) {
                throw new IllegalArgumentException("User ID and client ID must not be null");
            }

            // Create the index request to add the conversation to Elasticsearch
            IndexRequest<ConversationDocument> indexRequest = new IndexRequest.Builder<ConversationDocument>()
                    .index("conversations") // Elasticsearch index name
                    .id(conversation.getId()) // Optional: Set an explicit ID
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
}
