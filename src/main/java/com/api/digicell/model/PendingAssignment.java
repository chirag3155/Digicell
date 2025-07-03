package com.api.digicell.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class PendingAssignment implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String conversationId;
    private String clientId;
    private String assignedUserId;
    private String assignedUserName;
    private String assignedUserEmail;
    private String tenantId;
    
    // Request details
    private String summary;
    private String history;
    private String timestamp;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String clientLabel;
    
    // Assignment tracking
    private LocalDateTime assignmentTime;
    private LocalDateTime timeoutTime;
    private int currentRetryCount;
    private int maxRetryLimit;
    private String socketClientId; // Chat module socket ID for sending agent_ack
    private Set<String> triedUserIds = new HashSet<>(); // Track users who have been tried
    
    // Status tracking
    private String status; // PENDING, ACKNOWLEDGED, TIMEOUT, FAILED
    
    public PendingAssignment() {
        this.assignmentTime = LocalDateTime.now();
        this.currentRetryCount = 0;
        this.status = "PENDING";
    }
    
    public PendingAssignment(String conversationId, String clientId, String assignedUserId, 
                           String tenantId, int timeoutSeconds, int maxRetryLimit, String socketClientId) {
        this();
        this.conversationId = conversationId;
        this.clientId = clientId;
        this.assignedUserId = assignedUserId;
        this.tenantId = tenantId;
        this.maxRetryLimit = maxRetryLimit;
        this.socketClientId = socketClientId;
        this.timeoutTime = LocalDateTime.now().plusSeconds(timeoutSeconds);
    }
    
    public boolean isTimedOut() {
        return LocalDateTime.now().isAfter(timeoutTime);
    }
    
    public boolean canRetry() {
        return currentRetryCount < maxRetryLimit;
    }
    
    public void incrementRetry() {
        this.currentRetryCount++;
    }
    
    public void updateTimeout(int timeoutSeconds) {
        this.timeoutTime = LocalDateTime.now().plusSeconds(timeoutSeconds);
    }
    
    public void addTriedUser(String userId) {
        this.triedUserIds.add(userId);
    }
    
    public boolean hasTriedUser(String userId) {
        return this.triedUserIds.contains(userId);
    }
    
    public Set<String> getTriedUserIds() {
        return new HashSet<>(this.triedUserIds);
    }
} 