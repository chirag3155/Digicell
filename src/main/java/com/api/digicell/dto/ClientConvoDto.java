package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientConvoDto {
    private Long conversationId;
    private Long agentId;
    private String agentName;
    private String intent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String chatSummary;
    private List<List<ChatMessage>> chatHistory;

    @Data
    public static class ChatMessageDTO {
        private String timestamp;
        private String content;
        private String role;
    }

} 