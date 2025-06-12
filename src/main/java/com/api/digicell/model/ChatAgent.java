package com.api.digicell.model;

import lombok.Data;
import java.util.Comparator;

@Data
public class ChatAgent {
    private String agentId;
    private int currentUserCount;
    private boolean offlineRequested;
    private long lastPingTime;

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