package com.api.digicell.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // Log the Elasticsearch host and port being used
            logger.info("Connecting to Elasticsearch at {}:{}", elasticsearchHost, elasticsearchPort);

            // Create an HTTP client to connect to Elasticsearch
            RestClient restClient = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort)).build();
            
            // Log successful client creation
            logger.info("Elasticsearch client created successfully.");

            // Use RestClientTransport to connect to Elasticsearch
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            
            // Return the Elasticsearch client to interact with Elasticsearch
            return new ElasticsearchClient(transport);

        } catch (Exception e) {
            // Log any errors during client creation
            logger.error("Error creating Elasticsearch client: {}", e.getMessage());
            throw new RuntimeException("Failed to create Elasticsearch client", e);
        }
    }
}
