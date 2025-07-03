package com.api.digicell.dtos;

import com.api.digicell.entities.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Chat History Response")
public class ChatHistoryDTO {
    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User Name")
    private String userName;

    @Schema(description = "User Query")
    private String intent;

    @Schema(description = "Chat Summary")
    private String chatSummary;

    @Schema(description = "Chat History - List of message exchanges")
    private List<List<MessageDTO>> chatHistory;

    @Data
    public static class MessageDTO {
        @Schema(description = "Timestamp of the message")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private String timestamp;

        @Schema(description = "Content of the message")
        private String content;

        @Schema(description = "Role of the message sender (user/agent)")
        private String role;
    }
} 