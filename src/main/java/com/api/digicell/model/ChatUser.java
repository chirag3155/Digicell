package com.api.digicell.model;

import lombok.Data;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class ChatUser implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String email;
    private String ipAddress;
    private String userName;
    private String userLabel;
    private int currentClientCount;
    private boolean offlineRequested;
    private long lastPingTime;
    @JsonProperty("activeConversations")
    private Set<String> activeConversations = new HashSet<>();
    private static final Logger log = LoggerFactory.getLogger(ChatUser.class);

    public ChatUser() {
        // Default constructor for Redis deserialization
    }

    public ChatUser(String userId) {
        this.userId = userId;
        this.currentClientCount = 0;
        this.offlineRequested = false;
        this.lastPingTime = System.currentTimeMillis();
    }

    public void updatePingTime() {
        this.lastPingTime = System.currentTimeMillis();
    }

    public boolean isPingTimeout(long timeoutMillis) {
        return (System.currentTimeMillis() - lastPingTime) > timeoutMillis;
    }

    public void addConversation(String conversationId) {
        activeConversations.add(conversationId);
        this.currentClientCount = activeConversations.size(); // Keep count in sync
        
        // ✅ FIX: Ensure count is never negative
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT DETECTED in addConversation for user {}, resetting to activeConversations size: {}", userId, activeConversations.size());
            this.currentClientCount = activeConversations.size();
        }
    }

    public void removeConversation(String conversationId) {
        activeConversations.remove(conversationId);
        this.currentClientCount = activeConversations.size(); // Keep count in sync
        
        // ✅ FIX: Ensure count is never negative
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT DETECTED in removeConversation for user {}, resetting to 0", userId);
            this.currentClientCount = 0;
            activeConversations.clear(); // Clear set if count is inconsistent
        }
    }

    public void incrementClientCount() {
        this.currentClientCount++;
        
        // ✅ FIX: Validate increment result
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT AFTER INCREMENT for user {}, resetting to 1", userId);
            this.currentClientCount = 1;
        }
    }

    public void decrementClientCount() {
        if (this.currentClientCount > 0) {
            this.currentClientCount--;
        } else {
            // ✅ FIX: Log warning for invalid decrement attempts
            log.warn("⚠️ INVALID DECREMENT ATTEMPT for user {} - count is already {}", userId, this.currentClientCount);
            this.currentClientCount = 0; // Ensure it stays at 0
        }
    }
    
    /**
     * ✅ NEW: Synchronize count with active conversations set
     */
    public void synchronizeClientCount() {
        int actualCount = activeConversations.size();
        if (this.currentClientCount != actualCount) {
            log.warn("⚠️ COUNT MISMATCH for user {}: stored={}, actual={}, correcting to actual", 
                    userId, this.currentClientCount, actualCount);
            this.currentClientCount = actualCount;
        }
    }
    
    /**
     * ✅ NEW: Validate and fix any count inconsistencies
     */
    public void validateAndFixCount() {
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT DETECTED for user {}: {}, resetting to 0", userId, this.currentClientCount);
            this.currentClientCount = 0;
            activeConversations.clear();
        }
        
        // Ensure count matches active conversations
        synchronizeClientCount();
    }

    public Set<String> getActiveConversations() {
        return activeConversations;
    }

    public void setActiveConversations(Set<String> activeConversations) {
        this.activeConversations = activeConversations != null ? activeConversations : new HashSet<>();
    }

    /**
     * Post-deserialization cleanup to ensure data consistency
     * This method should be called after loading from Redis to ensure proper migration
     */
    public void postDeserializationCleanup() {
        // Ensure activeConversations is never null
        if (this.activeConversations == null) {
            this.activeConversations = new HashSet<>();
        }
        
        // Synchronize the client count with actual conversations
        synchronizeClientCount();
        
        log.debug("✅ Post-deserialization cleanup completed for user {}: {} active conversations", 
                userId, activeConversations.size());
    }
} 