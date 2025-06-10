package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationResponseDTO {
    private Long conversationId;
    private Long clientId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String intent;
    private String chatSummary;
    private List<List<ChatMessage>> chatHistory;
} 