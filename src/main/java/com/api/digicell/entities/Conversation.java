package com.api.digicell.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import com.api.digicell.converters.ChatHistoryConverter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "Conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversationId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false)
    private String intent;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * Stores the chronological list of exchanged messages as JSON in DB using {@link ChatHistoryConverter}.
     * The structure is List<List<ChatMessage>> where the outer list represents conversation sessions
     * and the inner list contains the messages within each session.
     */
    @Convert(converter = ChatHistoryConverter.class)
    @Column(columnDefinition = "json")
    private List<List<ChatMessage>> chatHistory;


    @Column(nullable = false)
    private String chatSummary;

  
} 