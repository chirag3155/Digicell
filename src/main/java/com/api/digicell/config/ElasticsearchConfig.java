package com.api.digicell.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.api.digicell.repository.elasticsearch")
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.enabled:true}")  // Default to true if not set
    private boolean elasticsearchEnabled;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    private ElasticsearchClient elasticsearchClient;

    // Bean to initialize ElasticsearchClient
    @Bean
    @Conditional(ElasticsearchEnabledCondition.class)  // Only create this bean if Elasticsearch is enabled
    public ElasticsearchClient elasticsearchClient() {
        if (!elasticsearchEnabled) {
            logger.warn("Elasticsearch is disabled, no client will be initialized.");
            return null;  // If Elasticsearch is disabled, return null
        }

        try {
            logger.info("Attempting to connect to Elasticsearch at {}", elasticsearchUri);

            // Initialize the RestClient and ElasticsearchTransport
            RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchUri)).build();
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            this.elasticsearchClient = new ElasticsearchClient(transport);

            logger.info("Elasticsearch client initialized successfully");

        } catch (Exception e) {
            logger.error("Elasticsearch is not running: {}", e.getMessage());
            this.elasticsearchClient = null;  // Gracefully fail, return null to indicate failure
        }

        return this.elasticsearchClient;  // Return null if client is not available
    }

    // Bean to return status message based on Elasticsearch availability
    @Bean
    public String elasticsearchStatusMessage() {
        if (elasticsearchClient == null) {
            return "Elasticsearch is not running, but the application has started.";
        }
        return "Elasticsearch is up and running!";
    }
}
