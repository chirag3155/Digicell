package com.api.digicell.controllers;

import com.api.digicell.dto.AgentCreateDTO;
import com.api.digicell.dto.AgentDetailsResponseDTO;
import com.api.digicell.dto.AgentStatusDTO;
import com.api.digicell.dto.AgentUpdateDTO;
import com.api.digicell.entities.Agent;
import com.api.digicell.entities.Client;
import com.api.digicell.exceptions.InvalidAgentStatusException;
import com.api.digicell.exceptions.ResourceNotFoundException;
import com.api.digicell.responses.ApiResponse;
import com.api.digicell.services.AgentService;
import com.api.digicell.services.ClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import com.api.digicell.responses.ResponseUtil;
import org.springframework.transaction.annotation.Transactional;
import com.api.digicell.mapper.AgentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Agent Management", description = "APIs for managing agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;
    private final ClientService clientService;
    private final AgentMapper agentMapper;
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    /**
     * Create a new agent.
     */
    @Operation(
        summary = "Create a new agent",
        description = "Creates a new agent with the provided information. Email and name are required fields.",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Agent>> createAgent(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @Valid @RequestBody AgentCreateDTO createDTO) {
        logger.info("Creating new agent with name: {}", createDTO.getName());
        try {
            Agent agent = agentService.createAgent(createDTO);
            logger.info("Successfully created agent with id: {}", agent.getAgentId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Agent created successfully", agent));
        } catch (InvalidAgentStatusException e) {
            logger.error("Invalid agent status while creating agent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error creating agent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error creating agent", null));
        }
    }

    /**
     * List all agents.
     */
    @Operation(
        summary = "Get all agents",
        description = "Retrieves a list of all agents",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<Agent>>> getAllAgents(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        logger.info("Fetching all agents");
        try {
            List<Agent> agents = agentService.getAllAgents();
            logger.info("Successfully retrieved {} agents", agents.size());
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agents retrieved successfully", agents));
        } catch (Exception e) {
            logger.error("Error fetching agents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching agents", null));
        }
    }

    /**
     * Get agent by ID.
     */
    @Operation(
        summary = "Get agent by ID",
        description = "Retrieves agent details by their ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Agent>> getAgentById(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long id) {
        logger.info("Fetching agent with id: {}", id);
        try {
            Agent agent = agentService.getAgentById(id);
            logger.info("Successfully retrieved agent with id: {}", id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agent retrieved successfully", agent));
        } catch (ResourceNotFoundException e) {
            logger.error("Agent not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching agent with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching agent", null));
        }
    }

    /**
     * Update agent details.
     */
    @Operation(
        summary = "Update an existing agent",
        description = "Updates the details of an existing agent by ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Agent>> updateAgent(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long id,
            @Valid @RequestBody AgentUpdateDTO updateDTO) {
        logger.info("Updating agent with id: {}", id);
        try {
            Agent updatedAgent = agentService.updateAgent(id, updateDTO);
            logger.info("Successfully updated agent with id: {}", id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agent updated successfully", updatedAgent));
        } catch (ResourceNotFoundException e) {
            logger.error("Agent not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (InvalidAgentStatusException e) {
            logger.error("Invalid agent status while updating agent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating agent with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating agent", null));
        }
    }

    /**
     * Update agent status.
     */
    @Operation(
        summary = "Update agent status",
        description = "Updates the status of an existing agent",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Agent>> updateAgentStatus(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long id,
            @Valid @RequestBody AgentStatusDTO statusDTO) {
        logger.info("Updating status for agent with id: {}", id);
        try {
            Agent updatedAgent = agentService.updateAgentStatus(id, statusDTO);
            logger.info("Successfully updated agent status to: {} for agent id: {}", statusDTO.getStatus(), id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agent status updated successfully", updatedAgent));
        } catch (ResourceNotFoundException e) {
            logger.error("Agent not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (InvalidAgentStatusException e) {
            logger.error("Invalid agent status while updating status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error updating agent status for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating agent status", null));
        }
    }

    /**
     * Delete agent.
     */
    @Operation(
        summary = "Delete an agent",
        description = "Deletes an agent by their ID",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteAgent(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long id) {
        logger.info("Deleting agent with id: {}", id);
        try {
            agentService.deleteAgent(id);
            logger.info("Successfully deleted agent with id: {}", id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agent deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            logger.error("Agent not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (IllegalStateException e) {
            logger.error("Cannot delete agent with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error deleting agent with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error deleting agent", null));
        }
    }

    /**
     * List Clients being handled by a specific agent.
     */
    @Operation(
        summary = "List clients by agent",
        description = "Retrieves a list of clients being handled by a specific agent",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{agent_id}/clients")
    public ResponseEntity<ApiResponse<List<Client>>> listClientsByAgent(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable("agent_id") @Positive(message = "agent_id must be positive") Long agentId) {
        List<Client> clients = clientService.getClientsByAgent(agentId);
        return ResponseUtil.listResponse(clients, "clients for agent");
    }

    /**
     * Fetch agent details including all conversations.
     */
    @Operation(
        summary = "Get agent details",
        description = "Fetches detailed information about an agent including all conversations",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/{agentId}/details")
    public ResponseEntity<ApiResponse<AgentDetailsResponseDTO>> getAgentDetails(
            @RequestHeader(name = "Authorization", required = false) String authToken,
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

    /**
     * Set agent status to AVAILABLE.
     */
    @Operation(
        summary = "Set agent as available",
        description = "Sets the status of an agent to AVAILABLE",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PatchMapping("/{id}/available")
    @Transactional
    public ResponseEntity<ApiResponse<Agent>> setAgentAvailable(
            @RequestHeader(name = "Authorization", required = false) String authToken,
            @PathVariable @Positive Long id) {
        logger.info("Setting agent with id: {} to AVAILABLE", id);
        try {
            Agent updatedAgent = agentService.setAgentAvailable(id);
            logger.info("Successfully set agent status to AVAILABLE for agent id: {}", id);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Agent status set to AVAILABLE", updatedAgent));
        } catch (ResourceNotFoundException e) {
            logger.error("Agent not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error setting agent status to AVAILABLE for id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error setting agent status", null));
        }
    }
} 