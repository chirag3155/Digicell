package com.api.digicell.controllers;

import com.api.digicell.dto.UserConversationDTO;
import com.api.digicell.entities.User;
import com.api.digicell.services.UserService;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.responses.UserDetailsResponse;
import com.api.digicell.responses.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * List all users.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> listAllUsers() {
        logger.info("Received request to list all users");
        try {
            List<User> users = userService.getAllUsers();
            logger.debug("Found {} users", users.size());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Users fetched successfully", users));
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching users", null));
        }
    }

    /**
     * Get user by id.
     */
    @GetMapping("/{user_id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        User user = userService.getUserById(userId);
        ApiResponse<User> response = new ApiResponse<>(HttpStatus.OK.value(), "User fetched successfully", user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get users filtered by assignment status.
     */
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<User>>> listUsersByAssignment(@RequestParam("status") boolean status) {
        List<User> users = userService.getUsersByAssignmentStatus(status);
        return ResponseUtil.listResponse(users, status? "assigned users" : "unassigned users");
    }

    /**
     * Fetch a user along with all conversation details.
     */
    @GetMapping("/{user_id}/details")
    public ResponseEntity<ApiResponse<UserDetailsResponse>> getUserDetails(
            @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        logger.info("Received request to get details for user: {}", userId);
        try {
            UserDetailsResponse details = userService.getUserDetails(userId);
            logger.debug("Successfully fetched details for user: {}", userId);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User details fetched successfully", details));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID provided: {}", userId);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching user details for id {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching user details", null));
        }
    }

    @GetMapping("/{user_id}/conversations")
    public ResponseEntity<ApiResponse<List<UserConversationDTO>>> getUserConversations(
            @PathVariable("user_id") @Positive(message = "user_id must be positive") Long userId) {
        logger.info("Received request to get conversations for user: {}", userId);
        try {
            List<UserConversationDTO> conversations = userService.getUserConversations(userId);
            logger.debug("Found {} conversations for user: {}", conversations.size(), userId);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "User conversations fetched successfully", conversations));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user ID provided: {}", userId);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching conversations for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching user conversations", null));
        }
    }
} 