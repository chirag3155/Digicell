package com.api.digicell.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Set<String> activeClients = new HashSet<>();
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

    public void addClient(String clientId) {
        activeClients.add(clientId);
        this.currentClientCount = activeClients.size(); // Keep count in sync
        
        // ✅ FIX: Ensure count is never negative
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT DETECTED in addClient for user {}, resetting to activeClients size: {}", userId, activeClients.size());
            this.currentClientCount = activeClients.size();
        }
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
        this.currentClientCount = activeClients.size(); // Keep count in sync
        
        // ✅ FIX: Ensure count is never negative
        if (this.currentClientCount < 0) {
            log.warn("⚠️ NEGATIVE COUNT DETECTED in removeClient for user {}, resetting to 0", userId);
            this.currentClientCount = 0;
            activeClients.clear(); // Clear set if count is inconsistent
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
     * ✅ NEW: Synchronize count with active clients set
     */
    public void synchronizeClientCount() {
        int actualCount = activeClients.size();
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
            activeClients.clear();
        }
        
        // Ensure count matches active clients
        synchronizeClientCount();
    }

    public Set<String> getActiveClients() {
        return activeClients;
    }

    public static class UserComparator implements Comparator<ChatUser> {
        @Override
        public int compare(ChatUser a1, ChatUser a2) {
            // First compare by offline request status
            if (a1.isOfflineRequested() != a2.isOfflineRequested()) {
                return a1.isOfflineRequested() ? 1 : -1;
            }
            // Then compare by current client count
            return Integer.compare(a1.getCurrentClientCount(), a2.getCurrentClientCount());
        }
    }
} 