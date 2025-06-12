//package com.api.digicell.config;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.ElasticsearchTransport;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//import org.apache.http.HttpHost;
//import org.elasticsearch.client.RestClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
//
//@Configuration
//@EnableElasticsearchRepositories(basePackages = "com.api.digicell.repository.elasticsearch")
//public class ElasticsearchConfig {
//
//    @Value("${elasticsearch.host}")
//    private String elasticsearchHost;
//
//    @Value("${elasticsearch.port}")
//    private int elasticsearchPort;
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient() {
//        // Create the low-level client
//        RestClient restClient = RestClient.builder(
//            new HttpHost(elasticsearchHost, elasticsearchPort, "http"))
//            .build();
//
//        // Create the transport with a Jackson mapper
//        ElasticsearchTransport transport = new RestClientTransport(
//            restClient, new JacksonJsonpMapper());
//
//        // Create the API client
//        return new ElasticsearchClient(transport);
//    }
//}
