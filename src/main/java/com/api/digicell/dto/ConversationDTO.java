package com.api.digicell.dto;

import com.api.digicell.entities.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long clientId;
    private Long agentId;
    private String intent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<List<ChatMessage>> chatHistory;
} 