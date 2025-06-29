package com.api.digicell.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatRoom implements Serializable {
    private static final long serialVersionUID = 1L;
    private String conversationId;
    private String userId;
    private String clientId;
    private String summary;
    private String history;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ChatMessage> messages;
    private boolean isActive;

    public ChatRoom() {
        // Default constructor for serialization
    }

    public ChatRoom(String conversationId, String userId, String clientId, String summary, String history) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.clientId = clientId;
        this.summary = summary;
        this.history = history;
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