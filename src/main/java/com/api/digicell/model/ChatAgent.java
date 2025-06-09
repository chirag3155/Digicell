package com.api.digicell.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAgent {
    private String agentId;
    private boolean appliedForBreak;
    private int currentUserCount;
    
    // Custom comparator for min-heap based on currentUserCount
    public static class AgentComparator implements java.util.Comparator<ChatAgent> {
        @Override
        public int compare(ChatAgent a1, ChatAgent a2) {
            return Integer.compare(a1.getCurrentUserCount(), a2.getCurrentUserCount());
        }
    }
} 