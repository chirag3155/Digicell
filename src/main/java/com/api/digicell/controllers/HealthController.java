package com.api.digicell.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

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
public class HealthController {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Simple health check - Is system running?
     * GET /api/v1/health
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> isSystemRunning() {
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
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Status check with basic info
     * GET /api/v1/health/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
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