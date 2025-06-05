package com.api.digicell.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    private LocalDateTime timestamp;
} 