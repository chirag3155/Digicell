package com.api.digicell.dtos;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientConversationDTO {
    private Long conversationId;
    private String agentId;
    private String agentName;
    private String query;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<List<ChatMessageDTO>> chatHistory;

    @Data
    public static class ChatMessageDTO {
        private String timestamp;
        private String content;
        private String role;
    }
}