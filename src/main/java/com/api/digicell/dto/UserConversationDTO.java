package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserConversationDTO {
    private Long conversationId;
    private Long agentId;
    private String agentName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String query;
    private List<List<ChatMessage>> chatHistory;
} 