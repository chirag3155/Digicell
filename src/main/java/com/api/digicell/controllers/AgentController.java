package com.api.digicell.controllers;

import com.api.digicell.entities.Agent;
import com.api.digicell.entities.User;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.responses.AgentDetailsResponse;
import com.api.digicell.services.AgentService;
import com.api.digicell.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import com.api.digicell.responses.ResponseUtil;
import com.api.digicell.dto.AgentDetailsResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final AgentService agentService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    /**
     * List all agents.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Agent>>> listAllAgents() {
        List<Agent> agents = agentService.getAllAgents();
        return ResponseUtil.listResponse(agents, "agents");
    }

    /**
     * List users being handled by a specific agent.
     */
    @GetMapping("/{agent_id}/users")
    public ResponseEntity<ApiResponse<List<User>>> listUsersByAgent(@PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId) {
        List<User> users = userService.getUsersByAgent(agentId);
        return ResponseUtil.listResponse(users, "users for agent");
    }

    /**
     * Fetch agent details including all conversations.
     */
    @GetMapping("/{agentId}/details")
    public ResponseEntity<ApiResponse<AgentDetailsResponseDTO>> getAgentDetails(
            @PathVariable @Positive(message = "agentId must be positive") Long agentId) {
        logger.info("Received request to get details for agent: {}", agentId);
        try {
            AgentDetailsResponseDTO response = agentService.getAgentDetails(agentId);
            return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK.value(),
                "Agent details fetched successfully",
                response
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching agent details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage(),
                null
            ));
        } catch (Exception e) {
            logger.error("Unexpected error fetching agent details: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                null
            ));
        }
    }
} 