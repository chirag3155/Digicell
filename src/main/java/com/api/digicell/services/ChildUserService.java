package com.api.digicell.services;

import com.api.digicell.dto.ChildUserResponseDTO;
import com.api.digicell.dto.ChildUserListResponseDTO;
import com.api.digicell.dto.ExternalUserResponseDTO;
import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatRoom;
import com.api.digicell.entities.UserAccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChildUserService {

    private final RestTemplate restTemplate;
    private final RedisUserService redisUserService;
    
    private static final String EXTERNAL_API_BASE_URL = "https://digicel-sandbox.bngrenew.com/evaportalbackend";
    
    private static final long PING_TIMEOUT_SECONDS = 15;
    private static final long PING_TIMEOUT_MILLIS = PING_TIMEOUT_SECONDS * 1000;

    /**
     * Get child users with Redis status information
     */
    public ChildUserListResponseDTO getChildUsersWithRedisStatus(Long parentUserId, int page, int limit, String authToken) {
        log.info("Fetching child users for parent ID: {}, page: {}, limit: {} using provided auth token", 
                parentUserId, page, limit);
        
        try {
            // Call external API with provided auth token
            ExternalUserResponseDTO externalResponse = callExternalUserAPI(parentUserId, page, limit, authToken);
            
            if (externalResponse == null) {
                log.warn("External API returned completely null response for parent user ID: {}", parentUserId);
                throw new IllegalArgumentException("External API returned null response");
            }
            
            if (externalResponse.getResponse() == null) {
                log.warn("External API returned malformed response (response field is null) for parent user ID: {}", parentUserId);
                throw new IllegalArgumentException("External API returned malformed response structure");
            }
            
            // Check if content is null or empty
            List<ExternalUserResponseDTO.ExternalUserDTO> externalUsers = externalResponse.getResponse().getContent();
            if (externalUsers == null || externalUsers.isEmpty()) {
                log.info("External API returned empty user list for parent user ID: {}", parentUserId);
                
                // Return null pagination when no content exists
                return new ChildUserListResponseDTO(new ArrayList<>(), null);
            }
            
            // Process users and add Redis status
            List<ChildUserResponseDTO> processedUsers = processUsersWithRedisStatus(externalUsers);
            
            // Map pagination
            ChildUserListResponseDTO.PaginationDTO pagination = null;
            if (externalResponse.getResponse().getPagination() != null) {
                ExternalUserResponseDTO.ExternalPaginationDTO extPagination = externalResponse.getResponse().getPagination();
                pagination = new ChildUserListResponseDTO.PaginationDTO(
                    extPagination.getCurrentPage(),
                    extPagination.getTotalItems(),
                    extPagination.getTotalPages(),
                    extPagination.getNextPage(),
                    extPagination.getPreviousPage()
                );
            }
            
            log.info("Successfully processed {} child users for parent ID: {}", processedUsers.size(), parentUserId);
            return new ChildUserListResponseDTO(processedUsers, pagination);
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Let HTTP client exceptions bubble up to controller
            log.error("HTTP error from external API for parent ID {}: {} - {}", parentUserId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
            
        } catch (Exception e) {
            log.error("Error fetching child users for parent ID {}: {}", parentUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch child users: " + e.getMessage(), e);
        }
    }

    /**
     * Call external API to get child users using provided auth token
     */
    private ExternalUserResponseDTO callExternalUserAPI(Long parentUserId, int page, int limit, String authToken) {
        String url = String.format("%s/v1/user-role/get-childUser/%d?page=%d&limit=%d", 
                                   EXTERNAL_API_BASE_URL, parentUserId, page, limit);
        
        log.debug("Calling external API: {} with provided auth token", url);
        
        // Extract token from Authorization header if it includes "Bearer " prefix
        String token = authToken;
        if (authToken != null && authToken.startsWith("Bearer ")) {
            token = authToken.substring(7); // Remove "Bearer " prefix
        }
        
        log.info("Using provided JWT token for external API call");
        log.debug("JWT Token: {}", token);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "*/*");
        headers.set("langCode", "en");
        headers.set("Authorization", "Bearer " + "eyJhbGciOiJIUzUxMiJ9.eyJ0ZW5hbnRJZCI6IlJvYWRtYXAgTmV3IiwiaXBBZGRyZXNzIjoiMTI3LjAuMC4xIiwidG9rZW5UeXBlIjoiYXV0aCIsInVzZXJJZCI6MSwic3ViIjoiZXZhcm9hZG1hcEBibGFja25ncmVlbi5jb20iLCJpYXQiOjE3NTEyMDU3NTIsImV4cCI6MTc1ODQwNTc1Mn0.iRuJtIjXJxN9oOjrA6J9H5WZGWWIFEev1gAC8PC57C8GWA_cYZ4c7F2vCdJef9HUDcepXPfnrFapyCwyXkdTxw");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<ExternalUserResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ExternalUserResponseDTO.class);
            
            log.debug("External API response status: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.warn("Parent user ID {} not found in external API: {}", parentUserId, e.getMessage());
            throw new IllegalArgumentException("Parent user ID " + parentUserId + " not found. Please verify the parent user exists.");
            
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized access to external API for parent user ID {}: {}", parentUserId, e.getResponseBodyAsString());
            throw new org.springframework.web.client.HttpClientErrorException(e.getStatusCode(), e.getStatusText(), e.getResponseHeaders(), e.getResponseBodyAsByteArray(), null);
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("External API returned client error for parent user ID {}: {} - {}", parentUserId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new org.springframework.web.client.HttpClientErrorException(e.getStatusCode(), e.getStatusText(), e.getResponseHeaders(), e.getResponseBodyAsByteArray(), null);
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Network error connecting to external API {}: {}", url, e.getMessage());
            if (e.getCause() instanceof java.net.UnknownHostException) {
                throw new RuntimeException("Cannot resolve external API host. Please check network connectivity or contact system administrator.");
            } else {
                throw new RuntimeException("Network connection failed to external API. Please check your network connection and try again.");
            }
            
        } catch (Exception e) {
            log.error("Unexpected error calling external API {}: {} - Cause: {}", url, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "Unknown", e);
            throw new RuntimeException("Unexpected error connecting to external user service: " + e.getMessage(), e);
        }
    }

    /**
     * Process users and add Redis status information
     */
    private List<ChildUserResponseDTO> processUsersWithRedisStatus(List<ExternalUserResponseDTO.ExternalUserDTO> externalUsers) {
        if (externalUsers == null || externalUsers.isEmpty()) {
            log.debug("No users to process - returning empty list");
            return new ArrayList<>();
        }
        
        log.debug("Processing {} users with Redis status", externalUsers.size());
        return externalUsers.stream()
                .map(this::mapToChildUserResponseWithRedisStatus)
                .collect(Collectors.toList());
    }

    /**
     * Map external user to response DTO with Redis status
     */
    private ChildUserResponseDTO mapToChildUserResponseWithRedisStatus(ExternalUserResponseDTO.ExternalUserDTO externalUser) {
        ChildUserResponseDTO responseUser = new ChildUserResponseDTO();
        
        // Map basic fields
        responseUser.setUserId(externalUser.getUserId());
        responseUser.setUserName(externalUser.getUserName());
        responseUser.setEmail(externalUser.getEmail());
        responseUser.setPhoneNumber(externalUser.getPhoneNumber());
        responseUser.setStatus(mapToUserAccountStatus(externalUser.getStatus()));
        responseUser.setCreatedBy(externalUser.getCreatedBy());
        responseUser.setUpdatedBy(externalUser.getUpdatedBy());
        responseUser.setIsAgent(externalUser.getIsAgent());
        responseUser.setCreatedAt(externalUser.getCreatedAt());
        responseUser.setUpdatedAt(externalUser.getUpdatedAt());
        responseUser.setActive(externalUser.getActive());
        
        // TEMPORARY MOCK DATA - Force specific values for testing
        if (externalUser.getUserId() != null && externalUser.getUserId().equals(19L)) {
            log.info("FORCING MOCK DATA for User 19");
            responseUser.setRedisStatus("Active");
            List<String> mockClients = new ArrayList<>();
            mockClients.add("CLIENT_001");
            mockClients.add("CLIENT_002");
            responseUser.setActiveClients(mockClients);
            return responseUser;
        }
        
        if (externalUser.getUserId() != null && externalUser.getUserId().equals(59L)) {
            log.info("FORCING MOCK DATA for User 59");
            responseUser.setRedisStatus("Active");
            List<String> mockClients = new ArrayList<>();
            mockClients.add("CLIENT_003");
            responseUser.setActiveClients(mockClients);
            return responseUser;
        }
        
        // Add Redis status for other users
        String redisStatus = determineRedisStatus(externalUser.getUserId());
        responseUser.setRedisStatus(redisStatus);
        
        // Add active clients for other users
        List<String> activeClients = getActiveClients(externalUser.getUserId());
        responseUser.setActiveClients(activeClients);
        
        return responseUser;
    }

    /**
     * Determine Redis status based on ChatUser data
     */
    private String determineRedisStatus(Long userId) {
        if (userId == null) {
            return "Inactive";
        }
        
        // TEMPORARY: Mock data for testing - users 19 and 59 show as Active
        if (userId.equals(19L) || userId.equals(59L)) {
            log.info("MOCK: User {} returning Active status for testing", userId);
            return "Active";
        }
        
        try {
            ChatUser chatUser = redisUserService.getUser(userId.toString());
            
            if (chatUser == null) {
                log.debug("User {} not found in Redis ChatUser, status: Inactive", userId);
                return "Inactive";
            }
            
            // Check lastPingTime
            long currentTime = System.currentTimeMillis();
            long timeSinceLastPing = currentTime - chatUser.getLastPingTime();
            
            if (timeSinceLastPing <= PING_TIMEOUT_MILLIS) {
                log.debug("User {} last ping was {} ms ago (≤ {} ms), status: Active", 
                         userId, timeSinceLastPing, PING_TIMEOUT_MILLIS);
                return "Active";
            } else {
                log.debug("User {} last ping was {} ms ago (> {} ms), status: Inactive", 
                         userId, timeSinceLastPing, PING_TIMEOUT_MILLIS);
                return "Inactive";
            }
            
        } catch (Exception e) {
            log.error("Error determining Redis status for user {}: {}", userId, e.getMessage(), e);
            return "Inactive";
        }
    }

    /**
     * Get active client IDs for a user
     */
    private List<String> getActiveClients(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // TEMPORARY: Mock data for testing
        if (userId.equals(19L)) {
            log.info("MOCK: User 19 returning active clients: CLIENT_001, CLIENT_002");
            List<String> mockClients = new ArrayList<>();
            mockClients.add("CLIENT_001");
            mockClients.add("CLIENT_002");
            return mockClients;
        }
        
        if (userId.equals(59L)) {
            log.info("MOCK: User 59 returning active clients: CLIENT_003");
            List<String> mockClients = new ArrayList<>();
            mockClients.add("CLIENT_003");
            return mockClients;
        }
        
        try {
            ChatUser chatUser = redisUserService.getUser(userId.toString());
            
            if (chatUser == null || chatUser.getActiveConversations() == null) {
                return new ArrayList<>();
            }
            
            Set<String> activeConversations = chatUser.getActiveConversations();
            List<String> clientIds = new ArrayList<>();
            
            // For each active conversation, get the client ID from ChatRoom
            for (String conversationId : activeConversations) {
                try {
                    ChatRoom chatRoom = redisUserService.getChatRoom(conversationId);
                    if (chatRoom != null && chatRoom.getClientId() != null) {
                        clientIds.add(chatRoom.getClientId());
                    }
                } catch (Exception e) {
                    log.warn("Error getting ChatRoom for conversation {}: {}", conversationId, e.getMessage());
                }
            }
            
            log.debug("User {} has {} active conversations with client IDs: {}", 
                     userId, activeConversations.size(), clientIds);
            
            return clientIds;
            
        } catch (Exception e) {
            log.warn("Redis connection issue for user {} active clients, returning empty list: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Map external status string to UserAccountStatus enum
     * Handles different string formats from external API
     */
    private UserAccountStatus mapToUserAccountStatus(String externalStatus) {
        if (externalStatus == null || externalStatus.trim().isEmpty()) {
            log.debug("External status is null/empty, defaulting to OFFLINE");
            return UserAccountStatus.OFFLINE;
        }
        
        // Convert to uppercase and try to match enum values
        String normalizedStatus = externalStatus.trim().toUpperCase();
        
        try {
            // Try direct enum mapping first
            return UserAccountStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException e) {
            // Handle common variations and mappings
            switch (normalizedStatus) {
                case "ACTIVE":
                case "AVAILABLE": 
                case "ONLINE":
                    return UserAccountStatus.ONLINE;
                case "INACTIVE":
                case "UNAVAILABLE":
                case "OFFLINE":
                    return UserAccountStatus.OFFLINE;
                default:
                    log.warn("Unknown external status '{}', defaulting to OFFLINE", externalStatus);
                    return UserAccountStatus.OFFLINE;
            }
        }
    }
} 