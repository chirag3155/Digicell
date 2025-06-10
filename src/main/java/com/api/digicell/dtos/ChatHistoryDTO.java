package com.api.digicell.dtos;

import com.api.digicell.entities.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Chat History Response")
public class ChatHistoryDTO {
    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent Name")
    private String agentName;

    @Schema(description = "User Query")
    private String intent;

    @Schema(description = "Chat Summary")
    private String chatSummary;

    @Schema(description = "Chat History - List of message exchanges")
    private List<List<MessageDTO>> chatHistory;

    @Data
    public static class MessageDTO {
        @Schema(description = "Timestamp of the message")
        private String timestamp;

        @Schema(description = "Content of the message")
        private String content;

        @Schema(description = "Role of the message sender (user/agent)")
        private String role;
    }
} 