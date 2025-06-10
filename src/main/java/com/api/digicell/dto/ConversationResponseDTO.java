package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationResponseDTO {
    private Long conversationId;
    private Long clientId;
    private String userName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private String intent;
    private String chatSummary;
    private List<List<ChatMessage>> chatHistory;
} 