package com.api.digicell.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatRoom {
    private String conversationId;
    private String userId;
    private String clientId;
    private String summary;
    private List<Map<String, Object>> history;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ChatMessage> messages;
    private boolean isActive;

    public ChatRoom(String conversationId, String userId, String clientId, String summary, List<Map<String, Object>> history) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.clientId = clientId;
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

    public String getRoomId() {
        return conversationId;
    }
} 