package com.api.digicell.controllers;

import com.api.digicell.dto.UserAccountCreateDTO;
import com.api.digicell.dto.UserAccountDetailsResponseDTO;
import com.api.digicell.dto.UserAccountStatusDTO;
import com.api.digicell.dto.UserAccountUpdateDTO;
import com.api.digicell.entities.UserAccount;
import com.api.digicell.entities.Client;
import com.api.digicell.exceptions.InvalidUserStatusException;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.services.UserAccountService;
import com.api.digicell.services.ClientService;
import com.api.digicell.services.ChildUserService;
import com.api.digicell.services.RedisUserService;
import com.api.digicell.dto.ChildUserListResponseDTO;
import com.api.digicell.dto.ChildUserRequestDTO;
import com.api.digicell.model.ChatUser;
import com.api.digicell.model.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.api.digicell.responses.ResponseUtil;
import org.springframework.transaction.annotation.Transactional;
import com.api.digicell.mapper.UserAccountMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final ClientService clientService;
    private final UserAccountMapper userAccountMapper;
    private final ChildUserService childUserService;
    private final RedisUserService redisUserService;
    private static final Logger logger = LoggerFactory.getLogger(UserAccountController.class);

    /**
     * Create a new user.
     */
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user with the provided information. Email and name are required fields.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<UserAccount>> createUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @Valid @RequestBody UserAccountCreateDTO createDTO) {
        logger.info("Creating new user with name: {}", createDTO.getName());
        try {
            UserAccount userAccount = userAccountService.createUser(createDTO);
            logger.info("Successfully created user with id: {}", userAccount.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(HttpStatus.CREATED.value(), "User created successfully", userAccount));
        } catch (InvalidUserStatusException e) {
            logger.error("Invalid user status while creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error creating user", null));
        }
    }

    /**
     * List all users.
     */
    @Operation(
        summary = "Get all users",
        description = "Retrieves a list of all users",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAccount>>> getAllUsers(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        logger.info("Fetching all users");
        try {
            List<UserAccount> userAccounts = userAccountService.getAllUsers();
            logger.info("Successfully retrieved {} users", userAccounts.size());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Users retrieved successfully", userAccounts));
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching users", null));
        }
    }

    /**
     * Get user by ID.
     */
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user details by their ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{user_id}")
    public ResponseEntity<ApiResponse<UserAccount>> getUserById(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long user_id) {
        logger.info("Fetching user with id: {}", user_id);
        try {
            UserAccount userAccount = userAccountService.getUserById(user_id);
            logger.info("Successfully retrieved user with id: {}", user_id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User retrieved successfully", userAccount));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found with id: {}", user_id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching user with id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching user", null));
        }
    }

    /**
     * Update user details.
     */
    @Operation(
        summary = "Update an existing user",
        description = "Updates the details of an existing user by user_id",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PutMapping("/{user_id}")
    @Transactional
    public ResponseEntity<ApiResponse<UserAccount>> updateUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long user_id,
            @Valid @RequestBody UserAccountUpdateDTO updateDTO) {
        logger.info("Updating user with user_id: {}", user_id);
        try {
            UserAccount updatedUserAccount = userAccountService.updateUser(user_id, updateDTO);
            logger.info("Successfully updated user with id: {}", user_id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User updated successfully", updatedUserAccount));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found with id: {}", user_id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (InvalidUserStatusException e) {
            logger.error("Invalid user status while updating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating user with id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating user", null));
        }
    }

    /**
     * Update user status.
     */
    @Operation(
        summary = "Update user status",
        description = "Updates the status of an existing user",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PatchMapping("/{user_id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<UserAccount>> updateUserStatus(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long user_id,
            @Valid @RequestBody UserAccountStatusDTO statusDTO) {
        logger.info("Updating status for user with user_id: {}", user_id);
        try {
            UserAccount updatedUserAccount = userAccountService.updateUserStatus(user_id, statusDTO);
            logger.info("Successfully updated user status to: {} for user id: {}", statusDTO.getStatus(), user_id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User status updated successfully", updatedUserAccount));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found with user_id: {}", user_id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (InvalidUserStatusException e) {
            logger.error("Invalid user status while updating status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating user status for user_id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating user status", null));
        }
    }

    /**
     * Delete user.
     */
    @Operation(
        summary = "Delete an user",
        description = "Deletes an user by their ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @DeleteMapping("/{user_id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long user_id) {
        logger.info("Deleting user with user_id: {}", user_id);
        try {
            userAccountService.deleteUser(user_id);
            logger.info("Successfully deleted user with id: {}", user_id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found with id: {}", user_id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (IllegalStateException e) {
            logger.error("Cannot delete user with id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error deleting user with id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error deleting user", null));
        }
    }

    /**
     * List Clients being handled by a specific user.
     */
    @Operation(
        summary = "List clients by user",
        description = "Retrieves a list of clients being handled by a specific user",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{user_id}/clients")
    public ResponseEntity<ApiResponse<List<Client>>> listClientsByUser(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        List<Client> clients = clientService.getClientsByUser(userId);
        return ResponseUtil.listResponse(clients, "clients for user");
    }

    /**
     * Fetch user details including all conversations.
     */
    @Operation(
        summary = "Get user details",
        description = "Fetches detailed information about an user including all conversations",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{user_id}/details")
    public ResponseEntity<ApiResponse<UserAccountDetailsResponseDTO>> getUserDetails(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive(message = "user_id must be positive") Long user_id) {
        logger.info("Received request to get details for user: {}", user_id);
        try {
            UserAccountDetailsResponseDTO response = userAccountService.getUserDetails(user_id);
            return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK.value(),
                "User details fetched successfully",
                response
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching user details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            logger.error("Unexpected error fetching user details: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                null
            ));
        }
    }

    /**
     * Set user status to AVAILABLE.
     */
    @Operation(
        summary = "Set user as available",
        description = "Sets the status of an user to AVAILABLE",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PatchMapping("/{user_id}/available")
    @Transactional
    public ResponseEntity<ApiResponse<UserAccount>> setUserAvailable(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long user_id) {
        logger.info("Setting user with id: {} to AVAILABLE", user_id);
        try {
            UserAccount updatedUserAccount = userAccountService.setUserONLINE(user_id);
            logger.info("Successfully set user status to AVAILABLE for user id: {}", user_id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User status set to AVAILABLE", updatedUserAccount));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found with id: {}", user_id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error setting user status to AVAILABLE for id {}: {}", user_id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error setting user status", null));
        }
    }

    /**
     * Add test data to Redis for testing child users API.
     */
    @Operation(
        summary = "Add test data to Redis",
        description = "Adds sample ChatUser and ChatRoom data to Redis for testing the child users API with active status",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping("/test/redis-data")
    public ResponseEntity<ApiResponse<String>> addTestRedisData(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        logger.info("Adding test data to Redis for child users API testing");
        
        try {
            // Create test users and rooms
            ChatUser chatUser1 = new ChatUser();
            ChatUser chatUser2 = new ChatUser();
            ChatRoom chatRoom1 = new ChatRoom();
            ChatRoom chatRoom2 = new ChatRoom();
            ChatRoom chatRoom3 = new ChatRoom();
            
            // Configure first test user (Active)
            chatUser1.setUserId("19");
            chatUser1.setUserName("Pankaj");
            chatUser1.setEmail("pankaj@blackngreen.com");
            chatUser1.setIpAddress("127.0.0.1");
            chatUser1.setLastPingTime(System.currentTimeMillis()); // Recent ping = Active
            chatUser1.setCurrentClientCount(2);
            chatUser1.setOfflineRequested(false);
            
            // Add active conversations
            Set<String> activeConversations1 = new HashSet<>();
            activeConversations1.add("conv_001");
            activeConversations1.add("conv_002");
            chatUser1.setActiveConversations(activeConversations1);
            
            // Configure second test user (Active)
            chatUser2.setUserId("59");
            chatUser2.setUserName("TestUser59");
            chatUser2.setEmail("testuser59@example.com");
            chatUser2.setIpAddress("127.0.0.1");
            chatUser2.setLastPingTime(System.currentTimeMillis() - 5000); // 5 seconds ago = Active
            chatUser2.setCurrentClientCount(1);
            chatUser2.setOfflineRequested(false);
            
            // Add active conversations
            Set<String> activeConversations2 = new HashSet<>();
            activeConversations2.add("conv_003");
            chatUser2.setActiveConversations(activeConversations2);
            
            // Configure ChatRooms
            chatRoom1.setConversationId("conv_001");
            chatRoom1.setClientId("CLIENT_001");
            chatRoom1.setUserId("19");
            
            chatRoom2.setConversationId("conv_002");
            chatRoom2.setClientId("CLIENT_002");
            chatRoom2.setUserId("19");
            
            chatRoom3.setConversationId("conv_003");
            chatRoom3.setClientId("CLIENT_003");
            chatRoom3.setUserId("59");
            
            // Add data to Redis using the injected service
            this.redisUserService.addUser(chatUser1);
            this.redisUserService.addUser(chatUser2);
            this.redisUserService.addChatRoom(chatRoom1);
            this.redisUserService.addChatRoom(chatRoom2);
            this.redisUserService.addChatRoom(chatRoom3);
            
            String message = "Test data added successfully: User 19 (Active, 2 clients), User 59 (Active, 1 client)";
            logger.info(message);
            
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), message, "Data added to Redis"));
            
        } catch (Exception e) {
            logger.error("Error adding test data to Redis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                                          "Error adding test data to Redis: " + e.getMessage(), null));
        }
    }

    /**
     * Verify Redis connectivity and check stored data.
     */
    @Operation(
        summary = "Verify Redis data",
        description = "Checks if the test data exists in Redis and verifies connectivity",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/test/redis-verify")
    public ResponseEntity<ApiResponse<String>> verifyRedisData(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        logger.info("Verifying Redis data and connectivity");
        
        try {
            StringBuilder result = new StringBuilder();
            
            // Check if users exist in Redis
            ChatUser user19 = this.redisUserService.getUser("19");
            ChatUser user59 = this.redisUserService.getUser("59");
            
            result.append("User 19: ").append(user19 != null ? "EXISTS" : "NOT FOUND").append("\n");
            if (user19 != null) {
                result.append("  - Last Ping: ").append(user19.getLastPingTime()).append("\n");
                result.append("  - Current Time: ").append(System.currentTimeMillis()).append("\n");
                result.append("  - Time Diff: ").append(System.currentTimeMillis() - user19.getLastPingTime()).append(" ms\n");
                result.append("  - Active Conversations: ").append(user19.getActiveConversations()).append("\n");
            }
            
            result.append("User 59: ").append(user59 != null ? "EXISTS" : "NOT FOUND").append("\n");
            if (user59 != null) {
                result.append("  - Last Ping: ").append(user59.getLastPingTime()).append("\n");
                result.append("  - Current Time: ").append(System.currentTimeMillis()).append("\n");
                result.append("  - Time Diff: ").append(System.currentTimeMillis() - user59.getLastPingTime()).append(" ms\n");
                result.append("  - Active Conversations: ").append(user59.getActiveConversations()).append("\n");
            }
            
            // Check ChatRooms
            ChatRoom room1 = this.redisUserService.getChatRoom("conv_001");
            ChatRoom room2 = this.redisUserService.getChatRoom("conv_002");
            ChatRoom room3 = this.redisUserService.getChatRoom("conv_003");
            
            result.append("ChatRoom conv_001: ").append(room1 != null ? "EXISTS (Client: " + room1.getClientId() + ")" : "NOT FOUND").append("\n");
            result.append("ChatRoom conv_002: ").append(room2 != null ? "EXISTS (Client: " + room2.getClientId() + ")" : "NOT FOUND").append("\n");
            result.append("ChatRoom conv_003: ").append(room3 != null ? "EXISTS (Client: " + room3.getClientId() + ")" : "NOT FOUND").append("\n");
            
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Redis verification completed", result.toString()));
            
        } catch (Exception e) {
            logger.error("Error verifying Redis data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                                          "Error verifying Redis data: " + e.getMessage(), null));
        }
    }

    /**
     * Simple Redis connection test.
     */
    @Operation(
        summary = "Test Redis connection",
        description = "Simple test to verify Redis connectivity from Java application",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/test/redis-ping")
    public ResponseEntity<ApiResponse<String>> testRedisConnection(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        logger.info("Testing Redis connection");
        
        try {
            // Test if we can check for existing keys
            boolean user19Exists = this.redisUserService.userKeyExists("19");
            boolean user59Exists = this.redisUserService.userKeyExists("59");
            
            // Get raw data to see what's stored
            Object rawUser19 = this.redisUserService.getRawUserData("19");
            Object rawUser59 = this.redisUserService.getRawUserData("59");
            
            StringBuilder result = new StringBuilder();
            result.append("Redis Connection Test Results:\n");
            result.append("User 19 key exists: ").append(user19Exists).append("\n");
            result.append("User 59 key exists: ").append(user59Exists).append("\n");
            result.append("Raw User 19 data type: ").append(rawUser19 != null ? rawUser19.getClass().getSimpleName() : "null").append("\n");
            result.append("Raw User 59 data type: ").append(rawUser59 != null ? rawUser59.getClass().getSimpleName() : "null").append("\n");
            
            if (rawUser19 != null) {
                result.append("Raw User 19 data: ").append(rawUser19.toString().substring(0, Math.min(100, rawUser19.toString().length()))).append("...\n");
            }
            
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), 
                "Redis connection test completed", result.toString()));
            
        } catch (Exception e) {
            logger.error("Redis connection test failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                                          "Redis connection failed: " + e.getMessage(), null));
        }
    }

    /**
     * Get child users with Redis status information.
     */
    @Operation(
        summary = "Get child users with Redis status",
        description = "Retrieves child users for a parent user ID with Redis status information including active/inactive status and active client IDs",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping("/{parent_user_id}/children")
    public ResponseEntity<ApiResponse<ChildUserListResponseDTO>> getChildUsersWithRedisStatus(
            HttpServletRequest request,
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("parent_user_id") @Positive(message = "parent_user_id must be positive") Long parentUserId,
            @Valid @RequestBody ChildUserRequestDTO requestDTO) {
        
        logger.info("Received request to get child users for parent ID: {}, page: {}, limit: {}", 
                   parentUserId, requestDTO.getPage(), requestDTO.getLimit());
        
        try {
            ChildUserListResponseDTO childUsers = childUserService.getChildUsersWithRedisStatus(
                parentUserId, 
                requestDTO.getPage(), 
                requestDTO.getLimit(), 
                authToken
            );
            logger.info("Successfully retrieved {} child users for parent ID: {}", 
                       childUsers.getContent() != null ? childUsers.getContent().size() : 0, parentUserId);
            
            return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK.value(), 
                "Child users retrieved successfully with Redis status", 
                childUsers
            ));
            
        } catch (HttpClientErrorException e) {
            logger.warn("External API returned HTTP error for parent user ID {}: {} - {}", 
                       parentUserId, e.getStatusCode(), e.getResponseBodyAsString());
            
            // Return the exact status code and response body from external API
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ApiResponse<>(e.getStatusCode().value(), 
                                          e.getResponseBodyAsString(), null));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for parent user ID {}: {}", parentUserId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
                    
        } catch (Exception e) {
            logger.error("Error retrieving child users for parent ID {}: {}", parentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                                          "Error retrieving child users with Redis status", null));
        }
    }

} 