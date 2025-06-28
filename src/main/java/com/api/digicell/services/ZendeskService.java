package com.api.digicell.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ZendeskService {

    private final WebClient webClient;

    @Value("${zendesk.api.url}")
    private String zendeskApiUrl;

    public ZendeskService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> assignAgentToTicket(String conversationId, String agentEmail, String summary, String clientName, String clientEmail, String clientPhone) {
        String url = zendeskApiUrl.trim() + "/assign";
        
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("agentEmail", agentEmail);
        body.put("summary", summary);
        
        // Add client details in the required format
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("name", clientName != null ? clientName : "Unknown");
        userDetails.put("email", clientEmail != null ? clientEmail : "unknown@example.com");
        userDetails.put("phoneNumber", clientPhone != null ? clientPhone : "N/A");
        body.put("userDetails", userDetails);

        log.info("📞 Calling Zendesk API to assign agent. URL: {}, Body: {}", url, body);

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ Zendesk API call successful. Response: {}", response))
                .doOnError(error -> log.error("❌ Zendesk API call failed: {}", error.getMessage()));
    }
} 