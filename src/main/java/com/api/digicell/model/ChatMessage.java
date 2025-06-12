package com.api.digicell.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private LocalDateTime timestamp;
    private String content;
    private String role; // "agent" or "user"
    private String roomId;
    private String senderId;
} 