package com.api.digicell.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Represents a single message exchanged in a conversation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /** "user" or "agent" */
    @JsonProperty("role")
    private String role;

    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
} 