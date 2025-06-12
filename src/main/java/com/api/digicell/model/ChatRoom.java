package com.api.digicell.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRoom {
    private String roomId;
    private String agentId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ChatMessage> messages;
    private boolean isActive;

    public ChatRoom(String roomId, String agentId, String userId) {
        this.roomId = roomId;
        this.agentId = agentId;
        this.userId = userId;
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