package com.api.digicell.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Health Check Controller for Digicell Module
 * Just checks if the system is running or not
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin(origins = "*")
@Tag(name = "Health Check", description = "APIs for system health monitoring and status checks")
@SecurityRequirement(name = "bearerAuth")
public class HealthController {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Simple health check - Is system running?
     * GET /api/v1/health
     */
    @Operation(
        summary = "System health check",
        description = "Checks if the Digicell system is running and responds with system status",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> isSystemRunning(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "UP");
            response.put("message", "Digicell system is running");
            response.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            response.put("system", "Digicell Chat API");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            response.put("status", "DOWN");
            response.put("message", "Digicell system is not running properly");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Simple ping - Just returns pong if running
     * GET /api/v1/health/ping
     */
    @Operation(
        summary = "Ping endpoint",
        description = "Simple ping endpoint that returns 'pong' if the system is responsive",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/ping")
    public ResponseEntity<String> ping(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        return ResponseEntity.ok("pong");
    }

    /**
     * Status check with basic info
     * GET /api/v1/health/status
     */
    @Operation(
        summary = "Detailed system status",
        description = "Returns detailed system status including version, uptime, and other system information",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestHeader(name = "Authorization", required = false) String authToken) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "RUNNING");
        response.put("application", "Digicell Chat API");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        response.put("uptime_seconds", getUptimeInSeconds());
        
        return ResponseEntity.ok(response);
    }

    // Helper method to get uptime in seconds
    private long getUptimeInSeconds() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }
} 