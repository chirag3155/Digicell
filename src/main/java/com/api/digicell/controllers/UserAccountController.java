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
import com.api.digicell.dto.ChildUserListResponseDTO;
import com.api.digicell.dto.ChildUserRequestDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @RequestHeader(name = "Authorization", required = true) String authToken,
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