package com.api.digicell.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatHistoryDTO {
    private Long conversationId;
    private Long agentId;
    private String agentName;
    private String intent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<List<MessageDTO>> chatHistory;

    @Data
    public static class MessageDTO {
        private String timestamp;
        private String content;
        private String role;
    }
} 