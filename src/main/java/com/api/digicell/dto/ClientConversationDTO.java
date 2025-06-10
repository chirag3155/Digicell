package com.api.digicell.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClientConversationDTO {
    private Long conversationId;
    private Long agentId;
    private String agentName;
    private String intent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

} 