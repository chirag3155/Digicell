package com.api.digicell.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private String roomId;
    private String userId;
    private String content;
    private String messageId;
    private String messageType;
    private LocalDateTime timestamp;
    private String role;  // "user" or "agent"
} 