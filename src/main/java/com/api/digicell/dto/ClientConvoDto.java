package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

@Data
public class ClientConvoDto {
    private Long conversationId;
    private Long clientId;
    private String clientName;
    private String intent;
    private List<String> labels;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
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