package com.api.digicell.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.api.digicell.repository.elasticsearch")
public class ElasticsearchConfig {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            logger.info("Elasticsearch URI: {}", elasticsearchUri);
            // Configure connection settings
            RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticsearchUri))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setConnectionTimeToLive(1, TimeUnit.MINUTES)
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(100))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(5000)  // 5 seconds
                    .setSocketTimeout(60000)  // 60 seconds
                    .setConnectionRequestTimeout(0));

            // Add failure listener
            builder.setFailureListener(new RestClient.FailureListener() {
                @Override
                public void onFailure(Node node) {
                    logger.warn("Node {} failed", node);
                }
            });

            RestClient restClient = builder.build();
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            logger.info("Elasticsearch client created successfully");
            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            logger.error("Failed to create Elasticsearch client: {}", e.getMessage());
            return null;  // Return null instead of throwing exception
        }
    }
}