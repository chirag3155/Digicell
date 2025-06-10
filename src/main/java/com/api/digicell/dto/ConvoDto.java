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
public class ConvoDto {

    private Long conversationId;
    private Long agentId;
    private String agentName;
    private String intent;
    private String chatSummary;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

} 