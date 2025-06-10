package com.api.digicell.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

@Data
public class ChatHistoryResponseDTO {
    private Long conversationId;
    private Long agentId;
    private String agentName;
    private String intent;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private List<List<MessageDTO>> chatHistory;

    @Data
    public static class MessageDTO {
        private String timestamp;
        private String content;
        private String role;
    }
}   