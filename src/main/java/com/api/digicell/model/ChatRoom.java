package com.api.digicell.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatRoom {
    private String roomId;
    private String agentId;
    private String userId;
    private String conversationId;
    private String summary;
    private List<Map<String, Object>> history;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ChatMessage> messages;
    private boolean isActive;

    public ChatRoom(String roomId, String agentId, String userId, String conversationId, String summary, List<Map<String, Object>> history) {
        this.roomId = roomId;
        this.agentId = agentId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.summary = summary;
        this.history = history != null ? new ArrayList<>(history) : new ArrayList<>();
        this.startTime = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.isActive = true;
    }

    public void addMessage(ChatMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }

    public void close() {
        this.endTime = LocalDateTime.now();
        this.isActive = false;
    }
} 