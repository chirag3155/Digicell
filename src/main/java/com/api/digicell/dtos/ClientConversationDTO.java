package com.api.digicell.dtos;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class ClientConversationDTO {
    private Long conversationId;
    private String userId;
    private String userName;
    private String query;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private List<List<ChatMessageDTO>> chatHistory;

    @Data
    public static class ChatMessageDTO {
        private String timestamp;
        private String content;
        private String role;
    }
}