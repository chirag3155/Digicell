package com.api.digicell.model;

import lombok.Data;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Data
public class ChatAgent {
    private String agentId;
    private String agentName;
    private String agentLabel;
    private int currentUserCount;
    private boolean offlineRequested;
    private long lastPingTime;
    private Set<String> activeUsers = new HashSet<>();

    public ChatAgent(String agentId) {
        this.agentId = agentId;
        this.currentUserCount = 0;
        this.offlineRequested = false;
        this.lastPingTime = System.currentTimeMillis();
    }

    public void updatePingTime() {
        this.lastPingTime = System.currentTimeMillis();
    }

    public boolean isPingTimeout(long timeoutMillis) {
        return (System.currentTimeMillis() - lastPingTime) > timeoutMillis;
    }

    public void addUser(String userId) {
        activeUsers.add(userId);
    }

    public void removeUser(String userId) {
        activeUsers.remove(userId);
    }

    public Set<String> getActiveUsers() {
        return activeUsers;
    }

    public static class AgentComparator implements Comparator<ChatAgent> {
        @Override
        public int compare(ChatAgent a1, ChatAgent a2) {
            // First compare by offline request status
            if (a1.isOfflineRequested() != a2.isOfflineRequested()) {
                return a1.isOfflineRequested() ? 1 : -1;
            }
            // Then compare by current user count
            return Integer.compare(a1.getCurrentUserCount(), a2.getCurrentUserCount());
        }
    }
} 