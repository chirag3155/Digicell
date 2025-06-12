package com.api.digicell.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.api.digicell.entities.ChatMessage;
import com.api.digicell.entities.Client;

import org.hibernate.annotations.CreationTimestamp;

import com.api.digicell.converters.ChatHistoryConverter;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Convert;
import lombok.Data;

@Data
public class ConDTO {

    private Client client;

    private UserAccountResponseDTO userAccountResponseDTO;

    private String intent;

    private String chatSummary;

     /**
     * Stores the chronological list of exchanged messages as JSON in DB using {@link ChatHistoryConverter}.
     * The structure is List<List<ChatMessage>> where the outer list represents conversation sessions
     * and the inner list contains the messages within each session.
     */
    @Convert(converter = ChatHistoryConverter.class)
    private List<List<ChatMessage>> chatHistory;

    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

}
