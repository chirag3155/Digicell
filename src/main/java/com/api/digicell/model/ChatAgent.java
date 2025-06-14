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
    private int currentClientCount;
    private boolean offlineRequested;
    private long lastPingTime;
    private Set<String> activeClients = new HashSet<>();

    public ChatAgent(String agentId) {
        this.agentId = agentId;
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
    }

    public void removeClient(String clientId) {
        activeClients.remove(clientId);
    }

    public Set<String> getActiveClients() {
        return activeClients;
    }

    public static class AgentComparator implements Comparator<ChatAgent> {
        @Override
        public int compare(ChatAgent a1, ChatAgent a2) {
            // First compare by offline request status
            if (a1.isOfflineRequested() != a2.isOfflineRequested()) {
                return a1.isOfflineRequested() ? 1 : -1;
            }
            // Then compare by current client count
            return Integer.compare(a1.getCurrentClientCount(), a2.getCurrentClientCount());
        }
    }
} 