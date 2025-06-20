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

            // Build search request - searching for real agent conversations
            // userId in controller refers to agent_id in real_agent_details
            // Double nested query: messages[] -> system_response[] -> real_agent_details.agent_id
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(QueryBuilders.nested(n -> n
                            .path("messages")
                            .query(QueryBuilders.nested(nested -> nested
                                    .path("messages.system_response")
                                    .query(QueryBuilders.term(t -> t.field("messages.system_response.real_agent_details.agent_id").value(userId.toString())))
                            ))
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for agent: " + userId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
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

            // Build search request - combining real agent and human client
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexName)
                    .query(QueryBuilders.bool(q -> q
                            .must(QueryBuilders.nested(n -> n
                                    .path("messages")
                                    .query(QueryBuilders.nested(nested -> nested
                                            .path("messages.system_response")
                                            .query(QueryBuilders.term(t -> t.field("messages.system_response.real_agent_details.agent_id").value(userId.toString())))
                                    ))
                            )) // Real agent
                            .must(QueryBuilders.term(t -> t.field("user_info.id.keyword").value(clientId))) // Human client
                    ))
                    .build();

            // Execute search
            SearchResponse<ConversationDocument> searchResponse = esClient.get().search(searchRequest, ConversationDocument.class);

            // Extract hits (conversation documents) from search response
            List<ConversationDocument> conversations = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            if (conversations.isEmpty()) {
                throw new ResourceNotFoundException("No conversations found for agent: " + userId + " and human client: " + clientId);
            }

            return conversations;
        } catch (IOException e) {
            logger.error("Error querying Elasticsearch: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch conversations: " + e.getMessage());
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
                    .index("convo") // Elasticsearch index name
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
