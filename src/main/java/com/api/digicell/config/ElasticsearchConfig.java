package com.api.digicell.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // Log the Elasticsearch host and port being used
            logger.info("Connecting to Elasticsearch at {}:{}", elasticsearchHost, elasticsearchPort);

            // Create credentials provider if username and password are provided
            RestClientBuilder builder = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, "https"));

            if (!elasticsearchUsername.isEmpty() && !elasticsearchPassword.isEmpty()) {
                logger.info("Using authentication for Elasticsearch with username: {}", elasticsearchUsername);
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));

                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    
                    // Disable SSL verification for development (use with caution in production)
                    try {
                        SSLContext sslContext = SSLContexts.custom()
                                .loadTrustMaterial(null, (x509Certificates, s) -> true)
                                .build();
                        httpClientBuilder.setSSLContext(sslContext);
                        httpClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true);
                    } catch (Exception e) {
                        logger.error("Error setting up SSL context: {}", e.getMessage());
                    }
                    
                    return httpClientBuilder;
                });
            }

            // Set timeouts
            builder.setRequestConfigCallback(requestConfigBuilder -> {
                return requestConfigBuilder
                        .setConnectTimeout(10000)
                        .setSocketTimeout(60000)
                        .setConnectionRequestTimeout(5000);
            });

            RestClient restClient = builder.build();
            
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