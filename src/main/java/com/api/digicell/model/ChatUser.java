package com.api.digicell.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
        this.currentClientCount = activeClients.size(); // Keep count in sync
    }

    public void incrementClientCount() {
        this.currentClientCount++;
    }

    public void decrementClientCount() {
        if (this.currentClientCount > 0) {
            this.currentClientCount--;
        }
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